package com.example.pastellmock.dto;

import com.example.pastellmock.domain.DocumentTransitions;
import com.example.pastellmock.domain.MockDocument;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reponse JSON renvoyee pour un GET sur
 * /api/v2/entite/{idEntite}/document/{idD}.
 *
 * Forme JSON cible :
 * <pre>
 * {
 *   "info": {
 *     "id_d": "0e2ebd294169",
 *     "id_e": 1,
 *     "type": "reservation-hoteliere",
 *     "last_action": "creation",
 *     "last_action_date": "2026-04-27 16:30:00"
 *   },
 *   "data": {},
 *   "action_possible": ["annulation", "validation"]
 * }
 * </pre>
 *
 * Champs :
 *   - info            : metadonnees du document (cf DocumentInfo)
 *   - data            : champs metier libres (vide a la creation)
 *   - action_possible : actions valides depuis l'etat courant.
 *                       Calcule via DocumentTransitions au Paquet 3
 *                       (auparavant, valeur hardcodee de demo).
 *
 * NOTE PAQUET 3 : la signature du factory method `from` a change.
 * Elle prend maintenant DocumentTransitions en parametre. Tous les callers
 * (DocumentController, tests) doivent etre adaptes.
 */
@JsonPropertyOrder({"info", "data", "action_possible"})
public record DetailDocumentResponse(

        @JsonProperty("info")
        DocumentInfo info,

        @JsonProperty("data")
        Map<String, String> data,

        @JsonProperty("action_possible")
        List<String> actionPossible

) {

    /**
     * Factory method : construit la reponse a partir d'un MockDocument.
     *
     * Au Paquet 3, action_possible est calcule dynamiquement via
     * DocumentTransitions. Avant, c'etait une valeur hardcodee de demo.
     *
     * @param doc         document a representer
     * @param transitions table des transitions, pour calculer les actions
     *                    valides depuis l'etat courant
     */
    public static DetailDocumentResponse from(MockDocument doc, DocumentTransitions transitions) {
        // Defensive copy : on n'expose pas la map mutable du domaine
        Map<String, String> dataCopy = new HashMap<>(doc.getData());

        // Actions calculees dynamiquement selon l'etat courant
        Set<String> actions = transitions.availableActions(doc.getLastAction());

        return new DetailDocumentResponse(
                DocumentInfo.from(doc),
                dataCopy,
                List.copyOf(actions)
        );
    }
}