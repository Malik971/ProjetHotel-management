package com.example.springhotel.integration.pastell.service;

import com.example.springhotel.entity.Reservation;
import com.example.springhotel.entity.Reservation.StatutReservation;
import com.example.springhotel.integration.pastell.client.PastellJournalEntry;
import com.example.springhotel.integration.pastell.config.PastellProperties;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.policy.PastellActionMapper;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Bean dedie au traitement transactionnel d'UNE entree du journal Pastell.
 *<p>
 * Pourquoi un bean separe et pas une methode dans PastellInboundSyncService ?
 *   - Spring AOP utilise des proxies dynamiques pour intercepter les appels et
 *     gerer les transactions. Quand une methode @Transactional est appelee
 *     DEPUIS LE MEME BEAN (auto-call via "this"), le proxy n'est PAS traverse,
 *     donc l'annotation est IGNOREE silencieusement. Risque grave en prod :
 *     un save() sur Reservation pourrait reussir et le save() sur PastellSync
 *     echouer, laissant la base dans un etat incoherent.
 *   - Pattern deja utilise par le Lot 4 : PastellRetryScheduler appelle
 *     pastellSyncService.retraiterSync() (autre bean), et l'annotation
 *     @Transactional REQUIRES_NEW est respectee. On reproduit la meme
 *     architecture pour le Lot 5 par coherence et par securite.
 *<p>
 * Responsabilites de ce bean :
 *   - Pour UNE entree, decider quoi faire (bascule statut, alignement,
 *     divergence, ignore) en deleguant la decision a {@link PastellActionMapper}.
 *   - Effectuer les saves dans une transaction unique (Reservation + PastellSync
 *     atomiques : les deux ou aucun).
 *<p>
 * Ce que ce bean NE fait PAS :
 *   - Aucune iteration sur des entrees : c'est l'orchestrateur ({@link PastellInboundSyncService})
 *     qui boucle.
 *   - Aucun acces au curseur : meme raison.
 *   - Aucun appel HTTP : meme raison.
 */
@Service
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellJournalEntryProcessor {

    private static final Logger log = LoggerFactory.getLogger(PastellJournalEntryProcessor.class);

    private final PastellSyncRepository pastellSyncRepository;
    private final ReservationRepository reservationRepository;
    private final PastellActionMapper actionMapper;
    private final PastellProperties properties;

    public PastellJournalEntryProcessor(
            PastellSyncRepository pastellSyncRepository,
            ReservationRepository reservationRepository,
            PastellActionMapper actionMapper,
            PastellProperties properties) {
        this.pastellSyncRepository = pastellSyncRepository;
        this.reservationRepository = reservationRepository;
        this.actionMapper = actionMapper;
        this.properties = properties;
    }

    /**
     * Traite UNE entree du journal Pastell, dans une transaction propre.
     *<p>
     * Etapes :
     *<p>
     *   1. Filtrage par entite : si l'idEntite ne correspond pas a celle configuree
     *      cote Sejour (pastell.entite-id), on ignore. Cas normal en pratique : un
     *      autre service tape sur le meme Pastell pour une autre entite.
     *<p>
     *   2. Recherche du PastellSync par idD :
     *      - Trouve : on continue le traitement.
     *      - Pas trouve : log WARN avec idD et idJ, on sort (decision Lot 5).
     *        Cas legitime : un dossier cree directement dans Studio sans passer
     *        par Sejour, ou un PastellSync purge.
     *<p>
     *   3. Detection de conflit : si l'action contredit le statut courant de la
     *      Reservation (ex. "annulee" sur une TERMINEE), on bascule SyncStatus
     *      en DIVERGENCE et on n'ecrase PAS le statut de la Reservation. Sejour
     *      reste autorite.
     *<p>
     *   4. Resolution du statut cible via PastellActionMapper :
     *      - Optional present, statut different : bascule de la Reservation.
     *      - Optional present, statut deja egal : alignement, on n'update que
     *        pastellEtatDernierConnu.
     *      - Optional vide : action neutre (creation, validee...), on n'update
     *        que pastellEtatDernierConnu.
     *<p>
     *   5. Sortie de DIVERGENCE : si le sync etait en DIVERGENCE et que l'action
     *      Pastell aligne maintenant les deux cotes, on remonte le sync en OK.
     *<p>
     * REQUIRES_NEW : chaque entree dans sa propre transaction, isolee des autres.
     * Si une entree echoue de maniere catastrophique, les suivantes du meme batch
     * doivent toujours pouvoir etre traitees. Le filet de securite est dans
     * l'orchestrateur (try/catch autour de l'appel a cette methode).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEntry(PastellJournalEntry entry) {
        // Etape 1 : filtre par entite
        long entiteAttendue = properties.getEntiteId();
        if (entry.idEntite() != entiteAttendue) {
            log.debug("Pastell polling : entree idJ={} idEntite={} ignoree (entite attendue = {})",
                    entry.idJ(), entry.idEntite(), entiteAttendue);
            return;
        }

        // Etape 2 : recherche du sync correspondant
        Optional<PastellSync> syncOpt = pastellSyncRepository.findByPastellDocumentId(entry.idD());
        if (syncOpt.isEmpty()) {
            // Decision Lot 5 : on log WARN et on continue, pas d'alerte.
            // Cas legitime : dossier cree directement dans Studio ou PastellSync purge.
            log.warn("Pastell polling : entree idJ={} avec idD={} action={} - aucun PastellSync correspondant, ignoree",
                    entry.idJ(), entry.idD(), entry.action());
            return;
        }
        PastellSync sync = syncOpt.get();

        // Etape 3 : recuperation de la reservation associee
        Optional<Reservation> reservationOpt = reservationRepository.findById(sync.getReservationId());
        if (reservationOpt.isEmpty()) {
            // Cas anormal : le PastellSync existe mais pas la reservation. Theoriquement
            // impossible grace a l'ON DELETE CASCADE, mais defensif au cas ou.
            log.warn("Pastell polling : PastellSync {} reference reservation {} introuvable - entree idJ={} ignoree",
                    sync.getId(), sync.getReservationId(), entry.idJ());
            return;
        }
        Reservation reservation = reservationOpt.get();
        StatutReservation statutCourant = reservation.getStatut();

        // Etape 4 : detection de conflit metier (DIVERGENCE)
        if (actionMapper.isConflict(entry.action(), statutCourant)) {
            log.warn("Pastell polling : DIVERGENCE detectee pour reservation {} (idD={}). " +
                            "Pastell envoie action='{}' mais statut Sejour='{}'. " +
                            "Sejour reste autorite, le sync passe en DIVERGENCE.",
                    reservation.getId(), entry.idD(), entry.action(), statutCourant);
            sync.setSyncStatus(SyncStatus.DIVERGENCE);
            sync.setPastellEtatDernierConnu(entry.action());
            sync.setDerniereSynchro(LocalDateTime.now());
            pastellSyncRepository.save(sync);
            return;
        }

        // Etape 5 : resolution du statut cible et bascule eventuelle
        Optional<StatutReservation> targetOpt = actionMapper.resolveTargetStatus(entry.action());
        if (targetOpt.isPresent() && targetOpt.get() != statutCourant) {
            StatutReservation target = targetOpt.get();
            log.info("Pastell polling : bascule reservation {} de {} vers {} (idD={}, idJ={}, action={})",
                    reservation.getId(), statutCourant, target, entry.idD(), entry.idJ(), entry.action());
            reservation.setStatut(target);
            reservationRepository.save(reservation);
        } else if (targetOpt.isPresent()) {
            log.debug("Pastell polling : reservation {} deja en {} (action={}, idJ={})",
                    reservation.getId(), statutCourant, entry.action(), entry.idJ());
        } else {
            log.debug("Pastell polling : action '{}' neutre pour reservation {} (idJ={})",
                    entry.action(), reservation.getId(), entry.idJ());
        }

        // Mise a jour systematique de l'etat connu cote sync.
        // Sortie de DIVERGENCE par alignement : si le sync etait en DIVERGENCE
        // et que la situation est maintenant coherente (action mappable + statut
        // courant egal a la cible), on remonte le sync en OK.
        sync.setPastellEtatDernierConnu(entry.action());
        if (sync.getSyncStatus() == SyncStatus.DIVERGENCE
                && targetOpt.isPresent()
                && targetOpt.get() == statutCourant) {
            log.info("Pastell polling : reservation {} sortie de DIVERGENCE par alignement",
                    reservation.getId());
            sync.setSyncStatus(SyncStatus.OK);
        }
        sync.setDerniereSynchro(LocalDateTime.now());
        pastellSyncRepository.save(sync);
    }
}