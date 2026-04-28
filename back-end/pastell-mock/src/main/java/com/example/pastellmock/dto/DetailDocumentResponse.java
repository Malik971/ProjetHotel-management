package com.example.pastellmock.dto;

import com.example.pastellmock.domain.MockDocument;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reponse JSON renvoyee pour un GET sur /api/v2/entite/{idEntite}/document/{idD}.
 *
 * Forme JSON cible (alignee sur la doc Pastell pour detail-document.php) :
 * <pre>
 * {
 *   "info": {
 *     "id_d": "5f3e8a9b2c1d",
 *     "id_e": 1,
 *     "type": "reservation-hoteliere",
 *     "last_action": "creation",
 *     "last_action_date": "2026-04-27 16:30:00"
 *   },
 *   "data": {
 *     "nom_client": "Dupont",
 *     "...": "..."
 *   },
 *   "action_possible": ["modification", "validation", "annulation"]
 * }
 * </pre>
 *
 * Champs :
 *   - info : metadonnees du document (cf DocumentInfo)
 *   - data : champs metier libres (vide a la creation, rempli au Paquet
 *     suivant via modify-document)
 *   - action_possible : liste des actions executables sur le document
 *     dans son etat actuel. Au Paquet 2, on retourne une liste statique
 *     basee sur lastAction. Au Paquet 3, on aura un vrai workflow.
 *
 * Decisions :
 *   - data : Map<String, String> pour rester proche du protocole Pastell
 *     qui ne modélise pas typeé les valeurs (tout est string en form-data)
 *   - action_possible : List<String> meme si actuellement on ne renvoie
 *     qu'une valeur, parce que la vraie API en retourne plusieurs
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
     * Pour action_possible, on renvoie une liste basee sur l'etat courant.
     * Au Paquet 2, le workflow n'est pas encore implemente, donc on renvoie
     * une liste constante. Le Paquet 3 introduira un vrai mecanisme de
     * transitions et action_possible reflectera les actions valides.
     */
    public static DetailDocumentResponse from(MockDocument doc) {
        // Defensive copy : on n'expose pas la map mutable du domaine.
        // Sans ca, un caller pourrait modifier le store en mutant cette map.
        Map<String, String> dataCopy = new HashMap<>(doc.getData());

        return new DetailDocumentResponse(
                DocumentInfo.from(doc),
                dataCopy,
                actionsPossiblesFor(doc.getLastAction())
        );
    }

    /**
     * Retourne les actions possibles selon l'etat courant.
     *
     * Implementation provisoire : tres simple, sera remplacee au Paquet 3
     * par un vrai moteur de workflow base sur les transitions decidees
     * pour le narratif (creation -> en-attente-validation -> validee ->
     * confirmee -> terminee, avec annulee atteignable depuis 3 etats).
     *
     * Pour l'instant : apres "creation", la seule action possible est
     * "modification" (qui sera implementee plus tard). On garde un seul
     * element dans la liste pour rester realiste vs vide.
     */
    private static List<String> actionsPossiblesFor(String lastAction) {
        if ("creation".equals(lastAction)) {
            return List.of("modification");
        }
        return List.of();
    }
}