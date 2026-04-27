package com.example.pastellmock.domain;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Modele de domaine d'un dossier Pastell stocke en memoire dans le mock.
 *
 * Ne PAS confondre avec :
 *   - Une entite JPA : ce n'est PAS persiste, c'est en RAM uniquement
 *   - Un DTO : ce n'est PAS un objet de transport HTTP, c'est l'objet "verite"
 *     du mock. Les DTOs (ce qu'on renvoie en JSON aux clients) sont dans le
 *     package dto/ et seront construits a partir de cet objet.
 *
 * Champs alignes sur la documentation Pastell officielle :
 *   - id_d         -> idD          (identifiant unique du document)
 *   - type         -> type         (type de dossier modélisé en Studio Pastell)
 *   - id_e         -> idEntite     (entite Pastell sur laquelle le doc est créé)
 *   - last_action       -> lastAction       (derniere action effectuee)
 *   - last_action_date  -> lastActionDate   (date de cette action)
 *   - data         -> data         (champs metier libres)
 *
 * Decision de design :
 *   - On utilise une classe MUTABLE plutot qu'un record immutable.
 *     Pourquoi ? Au Paquet 3, change-action.php devra MODIFIER lastAction et
 *     lastActionDate sur un dossier existant. Avec un record, il faudrait
 *     creer un nouveau MockDocument a chaque mutation et le re-stocker.
 *     Possible, mais plus complique. Une classe mutable + ConcurrentHashMap
 *     est la solution la plus simple et lisible.
 *
 *   - On utilise Lombok @Data pour eviter le boilerplate des getters/setters.
 *     Coherent avec le reste du projet.
 *
 *   - data est une Map<String, String> initialisee a vide. Pastell stocke
 *     des valeurs scalaires de formulaire. On utilisera une LinkedHashMap pour
 *     preserver l'ordre d'insertion (utile pour les tests deterministes).
 */
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MockDocument {

    /**
     * Identifiant unique du document. Genere par le store a la creation
     * (UUID tronque a 12 caracteres). Immuable une fois assigne.
     */
    private String idD;

    /**
     * Type de dossier (ex: "reservation-hoteliere"). En vrai Pastell ce serait
     * un nom symbolique modélisé en Studio. Pour le mock, on accepte n'importe
     * quelle chaine non vide.
     */
    private String type;

    /**
     * Identifiant numerique de l'entite Pastell sur laquelle le dossier est cree.
     * Toujours >= 1.
     */
    private long idEntite;

    /**
     * Derniere action effectuee. A la creation, vaut "creation".
     * Au Paquet 3, change-action.php pourra le faire passer a
     * "validation", "confirmation", "annulation", etc.
     */
    private String lastAction;

    /**
     * Date de la derniere action (creation ou changement d'action).
     * Stocke comme LocalDateTime, sera serialise au format
     * "yyyy-MM-dd HH:mm:ss" dans les reponses HTTP (convention Pastell).
     */
    private LocalDateTime lastActionDate;

    /**
     * Champs metier libres provenant du form-data envoye lors de la creation
     * (autres que `type`, qui est dans son propre champ ci-dessus).
     *
     * Au Paquet 2, ce sera vide a la creation : Pastell cree un dossier "vide"
     * puis on le modifie via modify-document.php. On garde ce champ des
     * maintenant pour anticiper.
     */
    private Map<String, String> data = new HashMap<>();
}