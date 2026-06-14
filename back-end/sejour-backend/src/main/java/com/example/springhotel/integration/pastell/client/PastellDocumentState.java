package com.example.springhotel.integration.pastell.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Etat courant d'un dossier Pastell tel que renvoye par le connecteur.
 * <p>
 * Couvre les deux reponses du mock (et du vrai Pastell) qui ont la meme forme :
 * </p>
 * <pre>
 * GET  /api/v2/entite/{e}/document/{idD}          -> detail-document
 * POST /api/v2/entite/{e}/document/{idD}/action   -> change-action
 *
 * {
 *   "result": "ok",                 (present uniquement sur change-action)
 *   "info": {
 *     "id_d": "0e2ebd294169",
 *     "id_e": 1,
 *     "type": "reservation-hoteliere",
 *     "last_action": "validee",
 *     "last_action_date": "2026-04-28 15:30:00"
 *   },
 *   "action_possible": ["confirmation", "annulation"]
 * }
 * </pre>
 * <p>
 * Les annotations {@link JsonProperty} valent dans les deux sens : a la
 * deserialisation depuis Pastell, et a la reserialisation lorsque le controller
 * de demonstration renvoie cet etat au dashboard. Ce dernier recoit donc la
 * meme forme snake_case que le mock, sans transformation cote serveur.
 * </p>
 * {@link JsonIgnoreProperties} avec {@code ignoreUnknown = true} protege contre
 * l'ajout futur de champs cote Pastell.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PastellDocumentState(
        String result,
        Info info,
        @JsonProperty("action_possible") List<String> actionPossible
) {

    /**
     * Bloc info du dossier : son identifiant, son etape circuit courante
     * (last_action) et l'horodatage de la derniere transition.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Info(
            @JsonProperty("id_d") String idD,
            @JsonProperty("id_e") Long idE,
            String type,
            @JsonProperty("last_action") String lastAction,
            @JsonProperty("last_action_date") String lastActionDate
    ) {
    }
}
