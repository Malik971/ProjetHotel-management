package com.example.pastellmock.dto;

import com.example.pastellmock.domain.JournalEntry;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * Reponse JSON pour UNE entree du journal Pastell.
 *
 * Le contoller GET /api/v2/journal renvoie une List<JournalEntryResponse>
 * (tableau JSON), pas un objet enveloppe. C'est ce que fait Pastell reel
 * et ce qu'attendra sejour-backend pour son polling au Lot 5.
 *
 * Forme JSON cible :
 * <pre>
 * {
 *   "id_j": 42,
 *   "id_d": "0e2ebd294169",
 *   "id_e": 1,
 *   "action": "validee",
 *   "date": "2026-04-28 15:30:00"
 * }
 * </pre>
 *
 * Le champ "action" porte l'ETAT cible apres transition (pas le verbe).
 * Voir le javadoc de JournalEntry pour la justification.
 *
 * Pourquoi un DTO separe de JournalEntry (domaine) ?
 *   - JournalEntry est mutable (Lombok @Data) : utilisable pour le store
 *   - JournalEntryResponse est immutable (record) : sur pour la sortie HTTP
 *   - Permet d'evoluer la forme JSON sans toucher au domaine
 */
@JsonPropertyOrder({"id_j", "id_d", "id_e", "action", "date"})
public record JournalEntryResponse(

        @JsonProperty("id_j")
        long idJ,

        @JsonProperty("id_d")
        String idD,

        @JsonProperty("id_e")
        long idEntite,

        @JsonProperty("action")
        String action,

        @JsonProperty("date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime date

) {

    /**
     * Factory : convertit une entree de journal du domaine en DTO HTTP.
     */
    public static JournalEntryResponse from(JournalEntry entry) {
        return new JournalEntryResponse(
                entry.getIdJ(),
                entry.getIdD(),
                entry.getIdEntite(),
                entry.getAction(),
                entry.getDate()
        );
    }
}