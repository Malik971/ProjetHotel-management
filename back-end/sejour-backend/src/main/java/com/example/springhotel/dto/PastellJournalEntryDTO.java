package com.example.springhotel.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entree du journal d'orchestration affichee sur la page detail d'un
 * dossier (Lot 3).
 * <p>
 * Refleté au plus pres ce que la table {@code pastell_journal_entry} contient,
 * en exposant explicitement les identifiants Pastell (id_d, id_j) pour que
 * les recruteurs Libriciel reconnaissent immediatement le vocabulaire de
 * l'API Pastell.
 */
@Builder
public record PastellJournalEntryDTO(
        Long id,
        Long idJ,
        String pastellDocumentId,
        Long idEntitePastell,
        String action,
        String fromState,
        String toState,
        LocalDateTime occurredAt,
        LocalDateTime recordedAt,
        String message,
        String severity
) {
}