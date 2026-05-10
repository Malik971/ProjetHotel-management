package com.example.springhotel.integration.pastell.service;

import com.example.springhotel.integration.pastell.client.PastellApiException;
import com.example.springhotel.integration.pastell.client.PastellClient;
import com.example.springhotel.integration.pastell.client.PastellJournalEntry;
import com.example.springhotel.integration.pastell.entity.PastellPollingCursor;
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
 * Role :
 *   - A chaque tick du {@link com.example.springhotel.integration.pastell.scheduler.PastellPollingScheduler},
 *     ce service est invoque. Il lit le curseur, demande au client les nouvelles
 *     entrees du journal Pastell, delegue le traitement de chaque entree au
 *     {@link PastellJournalEntryProcessor}, et avance le curseur.
 *<p>
 * Pourquoi cette separation orchestrateur / processor ?
 *   - Les transactions @Transactional ne fonctionnent qu'a travers les proxies
 *     Spring : un appel "this.processEntry()" depuis runPollOnce() bypass le
 *     proxy et ignore l'annotation. En externalisant processEntry dans un autre
 *     bean, on garantit que la transaction REQUIRES_NEW est bien creee.
 *   - Coherent avec le pattern Lot 4 : PastellRetryScheduler delegue a
 *     pastellSyncService.retraiterSync() qui est dans un autre bean.
 *<p>
 * Strategie transactionnelle :
 *   - {@link #runPollOnce()} N'EST PAS transactionnelle : elle orchestre l'appel
 *     HTTP (qui ne doit pas etre dans une transaction de longue duree) et la
 *     mise a jour du curseur, qui sont des operations distinctes.
 *   - {@link PastellJournalEntryProcessor#processEntry} est transactionnelle
 *     en REQUIRES_NEW.
 *   - {@link #updateCursor(long, LocalDateTime)} et
 *     {@link #updateCursorTimestampOnly(LocalDateTime)} sont marquees
 *     @Transactional REQUIRES_NEW pour le cas ou elles seraient appelees
 *     depuis un contexte transactionnel externe (test ou endpoint admin futur).
 *     Quand appelees depuis runPollOnce() (auto-call), l'annotation est ignoree
 *     par le proxy mais ces methodes ne font qu'un seul save() chacune, ce qui
 *     est atomique en JPA meme sans transaction explicite.
 *<p>
 * Strategie d'avancement du curseur :
 *   - Le curseur est avance au MAX idJ "vu", qu'il ait ete traite avec succes ou
 *     non. Une entree problematique (ex. PastellSync introuvable) n'est PAS
 *     bloquante : on log un WARN et on avance. Sinon, un cas non prevu pourrait
 *     bloquer tout le polling indefiniment.
 *   - Si le poll lui-meme echoue (HTTP ou reseau), le curseur n'est PAS modifie
 *     et on rejouera tout au prochain tick.
 *<p>
 * Conditional :
 *   - {@code pastell.enabled=true} : sans Pastell, ce service n'a pas de raison d'exister.
 *   - Le toggle {@code pastell.polling.enabled} est evalue cote scheduler, pas ici.
 *     Ainsi, si quelqu'un veut declencher manuellement runPollOnce() depuis un test
 *     ou un controleur d'admin, le service est dispo meme polling desactive.
 */
@Service
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellInboundSyncService {

    private static final Logger log = LoggerFactory.getLogger(PastellInboundSyncService.class);

    private final PastellClient pastellClient;
    private final PastellPollingCursorRepository cursorRepository;
    private final PastellJournalEntryProcessor entryProcessor;

    public PastellInboundSyncService(
            PastellClient pastellClient,
            PastellPollingCursorRepository cursorRepository,
            PastellJournalEntryProcessor entryProcessor) {
        this.pastellClient = pastellClient;
        this.cursorRepository = cursorRepository;
        this.entryProcessor = entryProcessor;
    }

    /**
     * Execute UNE passe de polling.
     *<p>
     * Etapes :
     *   1. Lire le curseur courant en base (ou 0 si absent).
     *   2. Appeler {@link PastellClient#fetchJournalSince(long)} pour recuperer
     *      les entrees plus recentes.
     *   3. Si vide : on rafraichit lastPolledAt et on sort.
     *   4. Sinon, pour chaque entree, deleguer au processor dans sa propre
     *      transaction. On track le max idJ vu.
     *   5. Avancer le curseur au max idJ vu et update lastPolledAt.
     *<p>
     * Cette methode est publique pour permettre :
     *   - Au scheduler de l'invoquer
     *   - A des tests d'integration de la declencher manuellement
     *   - A un eventuel endpoint d'admin (Lot 6) de forcer un poll a la demande
     *
     * @return le nombre d'entrees traitees avec succes (utile pour tests et observabilite)
     */
    public int runPollOnce() {
        // Etape 1 : lecture du curseur
        long sinceIdJ = cursorRepository.findCursor()
                .map(PastellPollingCursor::getLastProcessedIdJ)
                .orElse(0L);

        log.debug("Pastell polling : tick avec sinceIdJ={}", sinceIdJ);

        // Etape 2 : appel HTTP vers Pastell.
        // Si l'appel echoue, on log et on sort. Pas de bascule, pas de modif du curseur.
        // Au prochain tick, on retentera le meme appel : effet de retry par la frequence.
        List<PastellJournalEntry> entries;
        try {
            entries = pastellClient.fetchJournalSince(sinceIdJ);
        } catch (PastellApiException e) {
            log.warn("Pastell polling : echec appel journal (sinceIdJ={}) - {}", sinceIdJ, e.getMessage());
            return 0;
        }

        // Etape 3 : pas de nouveaute = sortie discrete + refresh lastPolledAt
        if (entries.isEmpty()) {
            log.debug("Pastell polling : aucune nouvelle entree depuis idJ={}", sinceIdJ);
            updateCursorTimestampOnly(LocalDateTime.now());
            return 0;
        }

        log.info("Pastell polling : {} nouvelle(s) entree(s) recuperee(s) depuis idJ={}",
                entries.size(), sinceIdJ);

        // Etape 4 : traitement entree par entree dans des transactions isolees
        long maxIdJVu = sinceIdJ;
        int traites = 0;
        for (PastellJournalEntry entry : entries) {
            try {
                entryProcessor.processEntry(entry);
                traites++;
            } catch (RuntimeException e) {
                // Filet de securite : on ne veut JAMAIS qu'une entree problematique
                // bloque tout le polling. Le curseur va quand meme avancer au-dela
                // de cette entree pour eviter une boucle infinie de retry sur la
                // meme entree cassee.
                log.error("Pastell polling : exception inattendue sur entree idJ={} idD={} - {}",
                        entry.idJ(), entry.idD(), e.getMessage(), e);
            }
            // On avance maxIdJVu meme en cas d'echec : c'est le prix a payer
            // pour eviter le blocage. La trace dans les logs reste pour le debug.
            if (entry.idJ() > maxIdJVu) {
                maxIdJVu = entry.idJ();
            }
        }

        // Etape 5 : avancement du curseur
        updateCursor(maxIdJVu, LocalDateTime.now());
        log.info("Pastell polling : passe terminee, {} entree(s) traitee(s), curseur avance a idJ={}",
                traites, maxIdJVu);
        return traites;
    }

    /**
     * Met a jour le curseur en base : avance lastProcessedIdJ et lastPolledAt.
     *<p>
     * REQUIRES_NEW pour les cas d'appel cross-bean (test, endpoint admin futur).
     * Lors de l'auto-call depuis runPollOnce(), l'annotation est ignoree par le
     * proxy mais ce n'est pas grave : la methode ne fait qu'un seul save() qui
     * sera de toute facon dans sa propre transaction implicite JPA.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCursor(long lastProcessedIdJ, LocalDateTime lastPolledAt) {
        Optional<PastellPollingCursor> cursorOpt = cursorRepository.findCursor();
        PastellPollingCursor cursor;
        if (cursorOpt.isPresent()) {
            cursor = cursorOpt.get();
        } else {
            // Cas tests sous H2 : la migration V4 ne tourne pas, on cree la ligne
            // a la volee. En prod, ce cas ne devrait jamais arriver puisque V4
            // a deja insere la ligne unique.
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

    /**
     * Met a jour uniquement le timestamp lastPolledAt (cas "rien de neuf").
     *<p>
     * On ne touche pas a lastProcessedIdJ pour ne pas creer un faux mouvement
     * dans les logs d'audit. Mais on actualise lastPolledAt pour garder une
     * trace que le polling tourne et que Pastell repond.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCursorTimestampOnly(LocalDateTime lastPolledAt) {
        cursorRepository.findCursor().ifPresent(cursor -> {
            cursor.setLastPolledAt(lastPolledAt);
            cursorRepository.save(cursor);
        });
    }
}