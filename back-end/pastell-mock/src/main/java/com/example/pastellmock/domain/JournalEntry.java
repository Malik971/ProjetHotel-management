package com.example.pastellmock.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Modele de domaine d'une entree du journal Pastell.
 *
 * Le journal trace TOUTES les transitions d'etat des documents Pastell,
 * dans l'ordre chronologique. Il sera lu par sejour-backend (Lot 5) via
 * un polling regulier sur GET /api/v2/journal pour detecter les changements
 * survenus cote Pastell qui n'ont pas ete inities par sejour-backend lui-meme.
 *
 * Champs alignes sur la nomenclature Pastell officielle :
 *   - id_j     -> idJ        (identifiant unique de l'entree, monotone croissant)
 *   - id_d     -> idD        (document concerne)
 *   - id_e     -> idEntite   (entite Pastell)
 *   - action   -> action     (action effectuee : "creation", "validation"...)
 *   - date     -> date       (horodatage)
 *
 * Pourquoi un idJ monotone croissant ?
 *   - Permet le polling efficace : "donne-moi tout ce qui est plus recent que idJ N"
 *   - Garantit l'ordre meme si deux entrees ont le meme timestamp a la milliseconde
 *   - Standard cote Pastell reel (qui utilise un compteur SQL AUTO_INCREMENT)
 *
 * Pourquoi un MockDocument-like en classe mutable ?
 *   - Coherent avec MockDocument (le code reste homogene)
 *   - Lombok @Data fournit getters/setters/equals/hashCode/toString
 *   - On utilisera plus tard un DTO immutable JournalEntryDTO pour l'exposition HTTP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    /**
     * Identifiant unique de l'entree, monotone croissant.
     * Genere par le store lors de l'ajout au journal.
     */
    private long idJ;

    /**
     * Identifiant du document concerne.
     */
    private String idD;

    /**
     * Identifiant numerique de l'entite Pastell.
     */
    private long idEntite;

    /**
     * Action effectuee. Exemples :
     *   - "creation"               (a la creation du document)
     *   - "en-attente-validation"  (apres action=validation depuis "creation")
     *   - "validee"
     *   - "confirmee"
     *   - "terminee"
     *   - "annulee"
     *
     * Note : la valeur stockee ici est l'ETAT cible apres transition,
     * pas l'ACTION declenchante (qui est "validation", "confirmation"...).
     * C'est ce que fait Pastell reel, pour que le journal reflete l'historique
     * des etats du document.
     */
    private String action;

    /**
     * Horodatage de l'evenement.
     */
    private LocalDateTime date;
}