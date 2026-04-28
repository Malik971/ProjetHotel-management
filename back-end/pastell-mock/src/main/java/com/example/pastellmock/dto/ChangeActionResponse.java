package com.example.pastellmock.dto;

import com.example.pastellmock.domain.DocumentTransitions;
import com.example.pastellmock.domain.MockDocument;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Set;

/**
 * Reponse JSON renvoyee apres un POST sur
 * /api/v2/entite/{idEntite}/document/{idD}/action.
 *
 * Forme JSON cible :
 * <pre>
 * {
 *   "result": "ok",
 *   "info": {
 *     "id_d": "0e2ebd294169",
 *     "id_e": 1,
 *     "type": "reservation-hoteliere",
 *     "last_action": "en-attente-validation",
 *     "last_action_date": "2026-04-28 15:30:00"
 *   },
 *   "action_possible": ["annulation", "validation"]
 * }
 * </pre>
 *
 * Pourquoi le champ "result" ?
 *   - Convention Pastell : "result": "ok" signale le succes
 *   - En cas d'echec (transition invalide), c'est l'ExceptionHandler qui
 *     intervient AVANT que cette reponse soit construite. Donc ce DTO ne
 *     porte JAMAIS de cas d'erreur : sa simple existence en sortie HTTP
 *     veut dire "ca a marche".
 *
 * Pourquoi inclure action_possible ?
 *   - Apres une transition reussie, le client sait directement quelles
 *     actions sont valides depuis le nouvel etat.
 *   - Evite un GET supplementaire pour le rafraichir.
 *   - Coherent avec ce que renvoie DetailDocumentResponse.
 */
@JsonPropertyOrder({"result", "info", "action_possible"})
public record ChangeActionResponse(

        @JsonProperty("result")
        String result,

        @JsonProperty("info")
        DocumentInfo info,

        @JsonProperty("action_possible")
        List<String> actionPossible

) {

    /**
     * Factory method : construit la reponse de succes apres un changeAction
     * effectivement applique sur le store.
     *
     * @param doc         document apres mutation (lastAction = nouvel etat)
     * @param transitions table des transitions, pour calculer les actions
     *                    desormais possibles depuis le nouvel etat
     */
    public static ChangeActionResponse from(MockDocument doc, DocumentTransitions transitions) {
        Set<String> actions = transitions.availableActions(doc.getLastAction());
        return new ChangeActionResponse(
                "ok",
                DocumentInfo.from(doc),
                List.copyOf(actions)
        );
    }
}