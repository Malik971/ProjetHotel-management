package com.example.springhotel.dto;

import java.time.Instant;
import java.util.List;

/**
 * Reponse de GET /api/client/reservations/{id}/timeline.
 * <p>
 * Expose l'etat de progression d'une reservation en vocabulaire neutre,
 * sans mention de Pastell. Le front React consomme ce DTO pour afficher
 * la timeline visuelle cote utilisateur.
 *
 * @param reservationId  id de la reservation
 * @param statut         statut metier de la reservation (EN_ATTENTE, CONFIRMEE, etc.)
 * @param etapes         liste ordonnee des etapes de la timeline
 */
public record ReservationTimelineDTO(
        Long reservationId,
        String statut,
        List<TimelineEtapeDTO> etapes
) {

    /**
     * Une etape de la timeline.
     *
     * @param ordre   position dans la timeline (1, 2, 3, 4)
     * @param label   libelle affiche a l'utilisateur (vocabulaire neutre, sans Pastell)
     * @param statut  DONE | CURRENT | PENDING | ERROR
     * @param date    date de passage a cet etat, null si pas encore atteint
     */
    public record TimelineEtapeDTO(
            int ordre,
            String label,
            String statut,
            Instant date
    ) {
    }
}