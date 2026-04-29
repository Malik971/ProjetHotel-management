package com.example.springhotel.integration.pastell.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reponse de l'appel POST /api/v2/entite/{idEntite}/document.
 *
 * Forme JSON cote Pastell (et cote mock) :
 * <pre>
 * {
 *   "id_d": "5f3e8a9b2c1d",
 *   "info": { "id_d": "...", "id_e": 1, "type": "...", ... }
 * }
 * </pre>
 *
 * Choix de mapping :
 *   - On ne mappe QUE le champ {@code id_d} a la racine.
 *   - Le bloc {@code info} contient des donnees de lecture (last_action, dates,
 *     etc.) qui ne nous interessent pas au Lot 3 : on stocke seulement l'id_d
 *     dans PastellSync.pastellDocumentId pour pouvoir retrouver le dossier plus tard.
 *   - L'annotation {@link JsonIgnoreProperties} avec {@code ignoreUnknown = true}
 *     garantit qu'ajouter de nouveaux champs cote Pastell ne cassera pas la deserialisation.
 *
 * Pourquoi un record et pas une classe ?
 *   - Immuable par construction : pas de risque qu'un test ou un service modifie
 *     accidentellement la valeur retournee par PastellClient.
 *   - Concis : pas de boilerplate getters / equals / hashCode / toString.
 *   - Coherent avec le style des DTOs cote pastell-mock (eux aussi en records).
 *
 * Pourquoi {@link JsonProperty} et pas une PropertyNamingStrategy globale ?
 *   - Une strategy globale s'appliquerait a TOUTES les serialisations Jackson
 *     du backend (controllers REST, DTOs metier, etc.) et casserait la convention
 *     camelCase utilisee partout ailleurs dans Sejour.
 *   - L'annotation au cas par cas est plus chirurgicale : seuls les DTOs Pastell
 *     subissent le mapping snake_case, le reste de l'application n'est pas impacte.
 *
 * @param idD identifiant unique du dossier Pastell nouvellement cree (champ id_d).
 *            Sera stocke dans PastellSync.pastellDocumentId pour garantir l'idempotence
 *            et permettre les futurs appels modify-document, change-action, etc.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PastellCreateDocumentResponse(
        @JsonProperty("id_d") String idD
) {
}