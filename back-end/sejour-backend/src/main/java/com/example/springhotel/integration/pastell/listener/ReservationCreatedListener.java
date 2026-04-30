package com.example.springhotel.integration.pastell.listener;

import com.example.springhotel.integration.pastell.service.PastellSyncService;
import com.example.springhotel.reservation.event.ReservationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener Spring qui reagit a {@link ReservationCreatedEvent} pour declencher
 * la synchronisation vers Pastell.
 *
 * Pourquoi {@link TransactionalEventListener} avec phase AFTER_COMMIT ?
 *   - Spring met les evenements en attente jusqu'au commit reussi de la
 *     transaction qui les a publies.
 *   - Si la creation de la reservation echoue (rollback), le listener N'EST
 *     PAS invoque : pas de dossier Pastell pour une reservation qui n'a pas survecu.
 *   - Si le commit reussit, le listener s'execute hors de la transaction
 *     d'origine, ce qui evite de propager d'eventuelles exceptions Pastell
 *     vers la transaction de la reservation.
 *
 * Pourquoi pas {@link org.springframework.context.event.EventListener} simple ?
 *   - Avec un EventListener simple, le listener s'execute DANS la transaction
 *     d'origine, AVANT le commit. Si Pastell echoue et qu'on n'attrape pas
 *     l'exception, la transaction rollback et la reservation est perdue.
 *   - Avec TransactionalEventListener AFTER_COMMIT, on est garanti que la
 *     reservation existe bien en base au moment ou on appelle Pastell.
 *
 * Pourquoi tres peu de logique ici ?
 *   - Ce listener est volontairement "stupide" : il delegue immediatement
 *     a PastellSyncService. Toute la logique metier (idempotence, persistance
 *     PENDING, gestion des erreurs) est dans le service.
 *   - Avantage : on peut tester PastellSyncService en isolation totale (Mockito),
 *     et le listener ne necessite qu'un test d'integration end-to-end.
 *
 * Bascule en async au Lot 4+ :
 *   Au Lot 4, on pourra ajouter {@code @Async} sur cette methode pour que
 *   l'appel Pastell n'attende pas la reponse de la reservation cote client.
 *   Avec @TransactionalEventListener + @Async, la reservation est commit,
 *   on retourne la reponse au client, ET ENSUITE Pastell est appele en
 *   tache de fond. UX optimale.
 */
@Component
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class ReservationCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationCreatedListener.class);

    private final PastellSyncService pastellSyncService;

    public ReservationCreatedListener(PastellSyncService pastellSyncService) {
        this.pastellSyncService = pastellSyncService;
    }

    /**
     * Reagit a la creation d'une reservation, apres commit en base.
     *
     * Toute exception levee ici est capturee et loggee : on ne veut JAMAIS
     * qu'un probleme cote Pastell remonte au client final, qui a deja recu
     * sa reponse de creation de reservation. Le service interne
     * (PastellSyncService) ne propage normalement aucune exception, mais
     * on ajoute ici un filet de securite pour les cas exotiques.
     *
     * @param event evenement publie par ReservationService apres save()
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCreated(ReservationCreatedEvent event) {
        Long reservationId = event.reservationId();
        log.debug("Pastell : evenement ReservationCreated recu pour reservation {}", reservationId);

        try {
            pastellSyncService.synchroniserCreation(reservationId);
        } catch (Exception e) {
            // Filet de securite : meme si PastellSyncService est cense ne pas
            // propager d'exception, on protege ici contre toute exception inattendue
            // (NullPointerException, problemes de classpath, etc.) pour ne JAMAIS
            // impacter le flow utilisateur. Le job de reprise (Lot 4) detectera
            // l'absence de PastellSync et reprendra la synchro plus tard.
            log.error("Pastell : exception inattendue pour reservation {} - {}",
                    reservationId, e.getMessage(), e);
        }
    }
}