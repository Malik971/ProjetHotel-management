package com.example.springhotel.dto;

import com.example.springhotel.integration.pastell.entity.SyncStatus;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Ligne du tableau "Dossiers en orchestration" cote admin.
 * <p>
 * Agrege en un seul DTO les infos utiles a l'affichage : id de la reservation,
 * client, hotel, statut technique du sync, etape Pastell courante, derniere
 * synchro et nombre de tentatives.
 * <p>
 * Vocabulaire choisi : "dossier" pour designer le couple reservation/sync,
 * "etape circuit" pour la phase Pastell, dans la continuite du vocabulaire
 * d'orchestration adopte au lot 3.
 */
@Builder
public record PastellSyncSummaryDTO(
        Long syncId,
        Long reservationId,
        String clientNom,
        String clientEmail,
        String hotelNom,
        String reservationStatut,
        SyncStatus syncStatus,
        String etapeCircuit,
        String pastellDocumentId,
        LocalDateTime derniereSynchro,
        int retryCount,
        String errorMessage
) {
}