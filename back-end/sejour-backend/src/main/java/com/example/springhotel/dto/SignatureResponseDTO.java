package com.example.springhotel.dto;

import com.example.springhotel.entity.Reservation.StatutReservation;
import java.time.LocalDateTime;

/**
 * Reponse du endpoint POST /api/admin/reservations/{id}/signer.
 *
 * reservationId      : id de la reservation signee
 * nouveauStatut      : SIGNATURE_APPOSEE (puis CONFIRMEE si auto-transition)
 * nomSignataire      : repris de la requete, confirme la persistance
 * signedAt           : horodatage UTC de la signature
 * pdfDisponible      : true si le PDF a ete genere et stocke
 *
 * Le frontend utilise ce DTO pour rafraichir l'affichage sans second appel.
 */
public record SignatureResponseDTO(
        Long reservationId,
        StatutReservation nouveauStatut,
        String nomSignataire,
        LocalDateTime signedAt,
        boolean pdfDisponible
) {}