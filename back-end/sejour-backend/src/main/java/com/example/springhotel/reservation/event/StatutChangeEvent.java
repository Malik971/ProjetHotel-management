package com.example.springhotel.reservation.event;

import com.example.springhotel.entity.Reservation.StatutReservation;

/**
 * Evenement publie apres chaque changement de statut d'une reservation.
 *
 * Pourquoi un evenement plutot qu'un appel direct a EmailService ?
 *   - Meme logique de decouplage que ReservationCreatedEvent : le service
 *     qui effectue la transition (SignatureService, ReservationService) ne
 *     sait rien du canal de notification. On peut ajouter SMS, webhook,
 *     analytics sans toucher aux services metier.
 *   - Avec @TransactionalEventListener AFTER_COMMIT, l'email n'est envoye
 *     que si la transition a bien ete persistee en base.
 *
 * @param reservationId  id de la reservation concernee
 * @param ancienStatut   statut avant la transition (null a la creation)
 * @param nouveauStatut  statut apres la transition
 */
public record StatutChangeEvent(
        Long reservationId,
        StatutReservation ancienStatut,
        StatutReservation nouveauStatut
) {
    public StatutChangeEvent {
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId ne peut pas etre null");
        }
        if (nouveauStatut == null) {
            throw new IllegalArgumentException("nouveauStatut ne peut pas etre null");
        }
    }
}