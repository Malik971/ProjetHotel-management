package com.example.springhotel.integration.pastell.service;

import com.example.springhotel.entity.Reservation;
import com.example.springhotel.integration.pastell.client.PastellApiException;
import com.example.springhotel.integration.pastell.client.PastellClient;
import com.example.springhotel.integration.pastell.client.PastellCreateDocumentResponse;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
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
 * Service d'orchestration de la synchronisation Reservation -> Pastell.
 *
 * Ce service est le pont entre :
 *   - Le domaine metier (Reservation, persistee en base)
 *   - Le client HTTP Pastell (PastellClient, pure couche transport)
 *   - L'etat de synchronisation (PastellSync, persiste en base)
 *
 * Responsabilites :
 *   - Garantir l'idempotence : une reservation ne genere JAMAIS deux dossiers Pastell.
 *   - Persister la trace de la synchro (PastellSync) avant tout appel HTTP, pour
 *     que le job de reprise (Lot 4) puisse retrouver les sync orphelins en cas
 *     de crash serveur entre la persistance et l'appel.
 *   - Convertir les erreurs HTTP en bascule vers EN_RETRY (sans propager
 *     d'exception au listener appelant), respect du principe "Pastell satellite,
 *     Spring autorite".
 *   - Logger chaque etape pour le diagnostic en prod.
 *
 * Ce que ce service NE fait PAS :
 *   - Aucune logique de retry : le retry exponentiel sera ajoute au Lot 4
 *     via Spring Retry. Ici, un appel = une tentative.
 *   - Aucune connaissance du protocole HTTP : delegue tout a PastellClient.
 *   - Aucune ecoute d'evenement : c'est le ReservationCreatedListener
 *     (Paquet 4) qui appellera cette methode.
 *
 * Conditional bean :
 *   Comme {@link PastellClient}, ce composant n'est instancie que si
 *   {@code pastell.enabled=true}. Quand l'integration est desactivee,
 *   ce bean est absent du contexte.
 *
 * Cycle de vie d'un PastellSync gere par ce service :
 * <pre>
 *   reservation creee
 *           │
 *           ▼
 *   PENDING (persiste, idD = null)
 *           │
 *           ▼
 *   appel pastellClient.createDocument()
 *      ┌────┴────┐
 *      │         │
 *    succes   echec
 *      │         │
 *      ▼         ▼
 *      OK     EN_RETRY
 *   (idD set)  (tentatives = 1, derniereErreur = ...)
 * </pre>
 */
@Service
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellSyncService {

    private static final Logger log = LoggerFactory.getLogger(PastellSyncService.class);

    private final PastellClient pastellClient;
    private final PastellSyncRepository pastellSyncRepository;
    private final ReservationRepository reservationRepository;

    public PastellSyncService(
            PastellClient pastellClient,
            PastellSyncRepository pastellSyncRepository,
            ReservationRepository reservationRepository) {
        this.pastellClient = pastellClient;
        this.pastellSyncRepository = pastellSyncRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Synchronise une reservation nouvellement creee avec Pastell.
     *
     * Etapes :
     * <ol>
     *   <li>Verification d'idempotence : si un PastellSync existe deja pour cette
     *       reservation, on skip (cas d'un evenement rejoue, redemarrage, etc.).</li>
     *   <li>Chargement de la reservation depuis la base (verification d'existence).</li>
     *   <li>Persistance d'un PastellSync en PENDING avant tout appel HTTP.
     *       Si le serveur crashe ici, la trace existe en base et le job de reprise
     *       (Lot 4) la retrouvera.</li>
     *   <li>Appel HTTP a Pastell.</li>
     *   <li>Mise a jour du PastellSync :
     *       <ul>
     *         <li>succes -> statut OK + pastellDocumentId rempli</li>
     *         <li>echec  -> statut EN_RETRY + tentatives = 1 + derniereErreur</li>
     *       </ul></li>
     * </ol>
     *
     * IMPORTANT : cette methode n'eleve JAMAIS d'exception en cas d'echec Pastell.
     * Le principe "Spring est autorite, Pastell est satellite" impose qu'un echec
     * de synchro NE rolle PAS la reservation. La reservation reste valide en base,
     * Pastell sera resynchronise plus tard par le job de reprise.
     *
     * Propagation REQUIRES_NEW : cette methode demarre sa propre transaction,
     * independante de celle qui a cree la reservation. Cela garantit que le
     * PastellSync est commit dans sa propre transaction, qu'il y ait succes ou
     * echec dans la suite de l'operation.
     *
     * @param reservationId identifiant de la reservation a synchroniser
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchroniserCreation(Long reservationId) {
        // Etape 1 : verification d'idempotence
        // Si un sync existe deja, c'est qu'on a deja fait le travail (ou tente).
        // Le job de reprise du Lot 4 prendra le relais s'il est en EN_RETRY.
        if (pastellSyncRepository.existsByReservationId(reservationId)) {
            log.info("Pastell : sync deja present pour reservation {} - skip create-document", reservationId);
            return;
        }

        // Etape 2 : chargement de la reservation
        // Si elle n'existe pas, c'est anormal (le listener nous appelle apres save),
        // mais on log et on sort sans crasher.
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            log.warn("Pastell : reservation {} introuvable - synchro abandonnee", reservationId);
            return;
        }

        // Etape 3 : persistance du PastellSync en PENDING avant tout appel HTTP
        // Cette trace permet au job de reprise (Lot 4) de retrouver les syncs
        // orphelins si le serveur crashe entre la persistance et l'appel.
        PastellSync sync = PastellSync.builder()
                .reservationId(reservationId)
                .syncStatus(SyncStatus.PENDING)
                .tentatives(0)
                .build();
        sync = pastellSyncRepository.save(sync);
        log.debug("Pastell : PastellSync {} persiste en PENDING pour reservation {}", sync.getId(), reservationId);

        // Etape 4 + 5 : appel HTTP et mise a jour du sync
        try {
            PastellCreateDocumentResponse response = pastellClient.createDocument();
            // Succes : on bascule en OK et on enregistre l'id_d recu
            sync.setPastellDocumentId(response.idD());
            sync.setSyncStatus(SyncStatus.OK);
            sync.setTentatives(1);
            sync.setDerniereSynchro(LocalDateTime.now());
            sync.setDerniereErreur(null);
            pastellSyncRepository.save(sync);
            log.info("Pastell : sync OK pour reservation {} -> id_d {}", reservationId, response.idD());

        } catch (PastellApiException e) {
            // Echec : on bascule en EN_RETRY, on incremente tentatives et on stocke l'erreur.
            // On NE propage PAS l'exception : la reservation reste valide en base.
            sync.setSyncStatus(SyncStatus.EN_RETRY);
            sync.setTentatives(1);
            sync.setDerniereSynchro(LocalDateTime.now());
            sync.setDerniereErreur(buildErrorMessage(e));
            pastellSyncRepository.save(sync);
            log.warn("Pastell : sync EN_RETRY pour reservation {} - {}", reservationId, e.getMessage());
        }
    }

    /**
     * Construit un message d'erreur compact pour la colonne derniere_erreur.
     *
     * Format : "[statusCode] message" ou "[NETWORK] message" si pas de reponse HTTP.
     * Le body Pastell n'est pas inclus pour eviter de doubler l'info,
     * il est deja tronque a 1000 caracteres dans PastellApiException.
     */
    private String buildErrorMessage(PastellApiException e) {
        String prefix = e.hasHttpResponse()
                ? "[" + e.getStatusCode() + "]"
                : "[NETWORK]";
        return prefix + " " + e.getMessage();
    }
}