package com.example.springhotel.integration.pastell.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Reponse JSON pour UNE entree du journal Pastell, telle que percue par le client Sejour.
 *<p>
 * Forme JSON attendue (alignee sur ce que le mock pastell-mock retourne) :
 * <pre>
 * {
 *   "id_j": 42,
 *   "id_d": "0e2ebd294169",
 *   "id_e": 1,
 *   "action": "validee",
 *   "date": "2026-04-28 15:30:00"
 * }
 * </pre>
 *<p>
 * Choix de mapping :
 *   - On mappe TOUS les champs renvoyes par le mock, parce qu'ils sont tous
 *     potentiellement utiles : idJ pour le curseur, idD pour retrouver la
 *     reservation, idEntite pour filtrer (on ignore les autres entites pour
 *     le moment, mais on logue), action pour le mapping vers StatutReservation,
 *     date pour le diagnostic et l'observabilite future.
 *   - L'annotation {@link JsonIgnoreProperties} garantit qu'ajouter des champs
 *     cote Pastell ne cassera pas la deserialisation (forward compatibility).
 *<p>
 * Pourquoi un record ?
 *   - Immuable : aucun risque qu'un service modifie accidentellement la valeur
 *     entre la lecture HTTP et le traitement.
 *   - Concis : pas de boilerplate getter / equals / hashCode / toString.
 *   - Coherent avec PastellCreateDocumentResponse et avec le style du module pastell-mock.
 *<p>
 * Pourquoi des @JsonProperty au lieu d'une PropertyNamingStrategy globale ?
 *   - Meme raisonnement que pour PastellCreateDocumentResponse (cf. son javadoc) :
 *     une strategy globale impacterait toute l'app. Annotation chirurgicale ici,
 *     uniquement sur les DTOs Pastell.
 *<p>
 * Format de la date :
 *   - Le mock renvoie "yyyy-MM-dd HH:mm:ss" (cf. JournalEntryResponse.java cote mock).
 *   - On utilise {@link JsonFormat} pour declarer ce pattern explicitement, sans
 *     dependre d'une config Jackson globale.
 *
 * @param idJ      identifiant unique monotone de l'entree journal cote Pastell.
 *                 Sert de curseur pour le polling : on retient le max et on demande
 *                 toujours "tout ce qui est plus recent que ca".
 * @param idD      identifiant du dossier Pastell concerne (cle pour retrouver le
 *                 PastellSync correspondant, donc la Reservation Sejour).
 * @param idEntite identifiant de l'entite Pastell. Sejour est mono-entite, on
 *                 ignore (et on logue) les entrees provenant d'une autre entite.
 * @param action   etat cible du document apres la transition (ex. "validee", "annulee",
 *                 "terminee"). Voir {@link com.example.springhotel.integration.pastell.policy.PastellActionMapper}
 *                 pour le mapping vers StatutReservation.
 * @param date     horodatage cote Pastell. Conserve a titre informatif, n'a pas
 *                 d'impact sur la logique d'ordonnancement (qui se fait par idJ).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PastellJournalEntry(

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
}