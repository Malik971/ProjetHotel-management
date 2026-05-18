package com.example.springhotel.integration.pastell.service;

import com.example.springhotel.integration.pastell.client.PastellApiException;
import com.example.springhotel.integration.pastell.client.PastellClient;
import com.example.springhotel.integration.pastell.client.PastellJournalEntry;
import com.example.springhotel.integration.pastell.entity.PastellJournalEntryRecord;
import com.example.springhotel.integration.pastell.entity.PastellPollingCursor;
import com.example.springhotel.integration.pastell.repository.PastellJournalEntryRecordRepository;
import com.example.springhotel.integration.pastell.repository.PastellPollingCursorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service d'orchestration du polling descendant Pastell -> Sejour (Lot 5).
 *<p>
 * <b>Evolution Lot 3 admin :</b>
 *   - Ajout de la sauvegarde locale du journal via PastellJournalEntryRecordRepository.
 *     Chaque entree traitee avec succes est persistee en base, ce qui permet a
 *     l'espace admin de reconstituer la frise d'orchestration d'un dossier sans
 *     re-interroger Pastell a chaque consultation.
 *<p>
 * Role (inchange) :
 *   - A chaque tick du PastellPollingScheduler, ce service est invoque. Il lit
 *     le curseur, demande au client les nouvelles entrees du journal Pastell,
 *     delegue le traitement de chaque entree au PastellJournalEntryProcessor,
 *     persiste la trace, et avance le curseur.
 */
@Service
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellInboundSyncService {

    private static final Logger log = LoggerFactory.getLogger(PastellInboundSyncService.class);

    private final PastellClient pastellClient;
    private final PastellPollingCursorRepository cursorRepository;
    private final PastellJournalEntryProcessor entryProcessor;
    private final PastellJournalEntryRecordRepository journalRecordRepository;

    public PastellInboundSyncService(
            PastellClient pastellClient,
            PastellPollingCursorRepository cursorRepository,
            PastellJournalEntryProcessor entryProcessor,
            PastellJournalEntryRecordRepository journalRecordRepository) {
        this.pastellClient = pastellClient;
        this.cursorRepository = cursorRepository;
        this.entryProcessor = entryProcessor;
        this.journalRecordRepository = journalRecordRepository;
    }

    public int runPollOnce() {
        long sinceIdJ = cursorRepository.findCursor()
                .map(PastellPollingCursor::getLastProcessedIdJ)
                .orElse(0L);

        log.debug("Pastell polling : tick avec sinceIdJ={}", sinceIdJ);

        List<PastellJournalEntry> entries;
        try {
            entries = pastellClient.fetchJournalSince(sinceIdJ);
        } catch (PastellApiException e) {
            log.warn("Pastell polling : echec appel journal (sinceIdJ={}) - {}", sinceIdJ, e.getMessage());
            return 0;
        }

        if (entries.isEmpty()) {
            log.debug("Pastell polling : aucune nouvelle entree depuis idJ={}", sinceIdJ);
            updateCursorTimestampOnly(LocalDateTime.now());
            return 0;
        }

        log.info("Pastell polling : {} nouvelle(s) entree(s) recuperee(s) depuis idJ={}",
                entries.size(), sinceIdJ);

        long maxIdJVu = sinceIdJ;
        int traites = 0;
        for (PastellJournalEntry entry : entries) {
            try {
                entryProcessor.processEntry(entry);
                // Sauvegarde locale de la trace journal (Lot 3 admin).
                // Idempotent : si l'idJ existe deja, on saute.
                saveJournalRecord(entry);
                traites++;
            } catch (RuntimeException e) {
                log.error("Pastell polling : exception inattendue sur entree idJ={} idD={} - {}",
                        entry.idJ(), entry.idD(), e.getMessage(), e);
            }
            if (entry.idJ() > maxIdJVu) {
                maxIdJVu = entry.idJ();
            }
        }

        updateCursor(maxIdJVu, LocalDateTime.now());
        log.info("Pastell polling : passe terminee, {} entree(s) traitee(s), curseur avance a idJ={}",
                traites, maxIdJVu);
        return traites;
    }

    /**
     * Persiste localement une entree journal Pastell pour la consultation admin.
     *<p>
     * Idempotent : si une entree avec ce idJ existe deja, on ne fait rien.
     * Ce cas peut arriver si le polling est redemarre apres un crash et que
     * le curseur n'a pas eu le temps de s'avancer.
     *<p>
     * Cette operation est dans sa propre micro-transaction (un seul save).
     * En cas d'echec, on log un WARN mais on ne bloque pas le polling :
     * la sauvegarde du journal est secondaire par rapport a l'avancement
     * du curseur metier.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveJournalRecord(PastellJournalEntry entry) {
        try {
            if (journalRecordRepository.existsByIdJ(entry.idJ())) {
                return;
            }
            PastellJournalEntryRecord record = PastellJournalEntryRecord.builder()
                    .idJ(entry.idJ())
                    .pastellDocumentId(entry.idD())
                    .action(entry.action())
                    .idEntitePastell(entry.idEntite())
                    .occurredAt(entry.date() != null ? entry.date() : LocalDateTime.now())
                    .recordedAt(LocalDateTime.now())
                    .severity(deriveSeverity(entry.action()))
                    .message(deriveMessage(entry.action(), entry.idD()))
                    .build();
            journalRecordRepository.save(record);
        } catch (RuntimeException e) {
            log.warn("Pastell polling : impossible de sauver la trace journal idJ={} - {}",
                    entry.idJ(), e.getMessage());
        }
    }

    /**
     * Deduit la severite d'une entree a partir de l'action.
     */
    private String deriveSeverity(String action) {
        if (action == null) return "INFO";
        return switch (action.toLowerCase()) {
            case "annulee" -> "WARN";
            case "validee", "confirmee", "terminee", "creation", "en-attente-validation" -> "INFO";
            default -> "INFO";
        };
    }

    /**
     * Deduit un message court a partir de l'action et de l'id_d.
     */
    private String deriveMessage(String action, String idD) {
        if (action == null) {
            return "Mise a jour du dossier";
        }
        return switch (action.toLowerCase()) {
            case "creation" -> "Dossier cree dans le bus Pastell";
            case "en-attente-validation" -> "Soumission au parapheur";
            case "validee" -> "Dossier valide par l'agent";
            case "confirmee" -> "Dossier confirme";
            case "terminee" -> "Cloture du dossier";
            case "annulee" -> "Dossier annule";
            default -> "Transition : " + action;
        };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCursor(long lastProcessedIdJ, LocalDateTime lastPolledAt) {
        Optional<PastellPollingCursor> cursorOpt = cursorRepository.findCursor();
        PastellPollingCursor cursor;
        if (cursorOpt.isPresent()) {
            cursor = cursorOpt.get();
        } else {
            log.warn("Pastell polling : curseur absent en base, creation a la volee");
            cursor = PastellPollingCursor.builder()
                    .id(PastellPollingCursorRepository.SINGLETON_ID)
                    .lastProcessedIdJ(0L)
                    .build();
        }
        cursor.setLastProcessedIdJ(lastProcessedIdJ);
        cursor.setLastPolledAt(lastPolledAt);
        cursorRepository.save(cursor);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCursorTimestampOnly(LocalDateTime lastPolledAt) {
        cursorRepository.findCursor().ifPresent(cursor -> {
            cursor.setLastPolledAt(lastPolledAt);
            cursorRepository.save(cursor);
        });
    }
}