package com.example.springhotel.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entree de la zone "Activite recente du bus" du dashboard admin.
 * <p>
 * Le frontend rend chaque entree avec une icone et un badge colore selon
 * le type. Pas de logique metier ici, juste un porte-information.
 *
 * @param type           OK | EN_RETRY | PENDING | DIVERGENCE | EN_ERREUR
 * @param title          ligne principale ("Dossier 42 valide")
 * @param subtitle       contexte ("Reservation Cassandre Martinez")
 * @param reservationId  pour pouvoir cliquer et naviguer vers le detail
 * @param occurredAt     timestamp de l'evenement
 */
@Builder
public record ActivityEntryDTO(
        String type,
        String title,
        String subtitle,
        Long reservationId,
        LocalDateTime occurredAt
) {
}