package com.example.pastellmock.dto;

import com.example.pastellmock.domain.MockDocument;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Reponse JSON renvoyee apres un POST sur /api/v2/entite/{idEntite}/document.
 *
 * Forme JSON cible :
 * <pre>
 * {
 *   "id_d": "5f3e8a9b2c1d",
 *   "info": {
 *     "id_d": "5f3e8a9b2c1d",
 *     "id_e": 1,
 *     "type": "reservation-hoteliere",
 *     "last_action": "creation",
 *     "last_action_date": "2026-04-27 16:30:00"
 *   }
 * }
 * </pre>
 *
 * Pourquoi id_d apparait deux fois (a la racine ET dans info) ?
 *   - C'est une bizarrerie historique du protocole Pastell
 *   - La doc officielle indique que create-document retourne id_d a la racine
 *   - On le duplique dans info pour rester coherent avec detail-document,
 *     qui lui retourne tout dans un bloc info
 *   - On respecte le contrat tel quel : ce n'est pas notre role de "corriger"
 *     l'API qu'on imite. Un mock fidele reproduit aussi les bizarreries.
 */
@JsonPropertyOrder({"id_d", "info"})
public record CreateDocumentResponse(

        @JsonProperty("id_d")
        String idD,

        @JsonProperty("info")
        DocumentInfo info

) {

    /**
     * Factory method : construit la reponse complete a partir d'un MockDocument.
     */
    public static CreateDocumentResponse from(MockDocument doc) {
        return new CreateDocumentResponse(
                doc.getIdD(),
                DocumentInfo.from(doc)
        );
    }
}