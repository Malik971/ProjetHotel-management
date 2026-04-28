package com.example.pastellmock.dto;

import com.example.pastellmock.domain.MockDocument;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * Bloc "info" present dans les reponses Pastell pour create-document
 * et detail-document.
 *
 * Forme JSON cible (convention Pastell, snake_case) :
 * <pre>
 * {
 *   "id_d": "5f3e8a9b2c1d",
 *   "id_e": 1,
 *   "type": "reservation-hoteliere",
 *   "last_action": "creation",
 *   "last_action_date": "2026-04-27 16:30:00"
 * }
 * </pre>
 *
 * Pourquoi un record ?
 *   - Immutable par construction : un DTO de sortie ne doit jamais etre mute
 *   - Genere automatiquement constructeur, getters, equals, hashCode, toString
 *   - Java 21 supporte nativement, pas besoin de Lombok
 *
 * Pourquoi @JsonProperty sur chaque champ ?
 *   - Pastell utilise snake_case ("id_d"), Java utilise camelCase ("idD")
 *   - On mappe explicitement, sans dependre d'une convention globale Jackson
 *     (ce qui evite des surprises si la config Jackson change ailleurs)
 *
 * Pourquoi @JsonPropertyOrder ?
 *   - L'ordre des cles JSON est preserve dans la reponse
 *   - Facilite le diff visuel avec une vraie reponse Pastell
 *   - Purement cosmetique mais "DevRel-grade"
 *
 * Pourquoi @JsonFormat sur la date ?
 *   - Format exige par Pastell : "yyyy-MM-dd HH:mm:ss"
 *   - Sans cette annotation, Jackson serialiserait LocalDateTime au format ISO
 *     ("2026-04-27T16:30:00") qui ne respecte pas le contrat Pastell
 */
@JsonPropertyOrder({"id_d", "id_e", "type", "last_action", "last_action_date"})
public record DocumentInfo(

        @JsonProperty("id_d")
        String idD,

        @JsonProperty("id_e")
        long idEntite,

        @JsonProperty("type")
        String type,

        @JsonProperty("last_action")
        String lastAction,

        @JsonProperty("last_action_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastActionDate

) {

    /**
     * Factory method : construit un DocumentInfo a partir d'un MockDocument.
     *
     * Pourquoi une methode statique ici plutot qu'un mapper separe ?
     *   - Centraliser la conversion domaine -> DTO a un seul endroit
     *   - Eviter de polluer MockDocument avec une dependance vers le DTO
     *     (le domaine ne doit jamais connaitre la couche transport)
     *   - Simple a tester et a faire evoluer
     */
    public static DocumentInfo from(MockDocument doc) {
        return new DocumentInfo(
                doc.getIdD(),
                doc.getIdEntite(),
                doc.getType(),
                doc.getLastAction(),
                doc.getLastActionDate()
        );
    }
}