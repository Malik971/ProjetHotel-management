package com.example.pastellmock.domain;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Table des transitions valides du workflow Pastell mock.
 *
 * Workflow simule (decide au Lot 1) :
 * <pre>
 *
 *      creation
 *        |
 *        | action=validation                     action=annulation
 *        |---------------------------+--------------------+
 *        |                           |                    |
 *        v                           v                    |
 *   en-attente-validation         annulee                 |
 *        |                                                |
 *        | action=validation                              |
 *        |---------------------------+--------------------+
 *        |                           |                    |
 *        v                           v                    |
 *      validee                    annulee                 |
 *        |                                                |
 *        | action=confirmation                            |
 *        |---------------------------+--------------------+
 *        |                           |                    |
 *        v                           v                    |
 *     confirmee                   annulee                 |
 *        |                                                |
 *        | action=terminaison                             |
 *        v                                                |
 *      terminee  (etat terminal, plus aucune transition)  |
 *
 * </pre>
 *
 * Pourquoi une classe @Component dediee ?
 *   - SRP (Single Responsibility) : la connaissance du workflow vit dans UN
 *     seul endroit. Le store ne sait pas quelles transitions sont valides,
 *     il delegue a cette classe.
 *   - Testable isolement, sans Spring, sans store : un test pur passe une
 *     paire (etat, action) et verifie le resultat.
 *   - Evolutif : ajouter un etat ou une action revient a modifier UNE map,
 *     pas a chasser les conditions if/else dans plusieurs fichiers.
 *
 * Pourquoi @Component plutot qu'une classe utilitaire statique ?
 *   - Pour pouvoir l'injecter dans le DocumentController (Vague 3) via
 *     constructeur, comme MockDocumentStore.
 *   - Permet plus tard de remplacer cette implementation par une autre
 *     (par exemple chargee depuis un fichier YAML) sans changer les callers.
 *   - Coherent avec le reste du projet.
 */
@Component
public class DocumentTransitions {

    // Etats du workflow (constantes pour eviter les "magic strings")
    public static final String CREATION = "creation";
    public static final String EN_ATTENTE_VALIDATION = "en-attente-validation";
    public static final String VALIDEE = "validee";
    public static final String CONFIRMEE = "confirmee";
    public static final String TERMINEE = "terminee";
    public static final String ANNULEE = "annulee";

    // Actions declenchantes
    public static final String ACTION_VALIDATION = "validation";
    public static final String ACTION_CONFIRMATION = "confirmation";
    public static final String ACTION_TERMINAISON = "terminaison";
    public static final String ACTION_ANNULATION = "annulation";

    /**
     * Table des transitions :
     * cle  = etat courant
     * valeur = map(action -> etat cible)
     *
     * LinkedHashMap pour preserver l'ordre d'insertion : utile uniquement pour
     * que availableActions() retourne les actions dans un ordre stable et
     * lisible (validation avant annulation par exemple). Un TreeMap aurait fait
     * un tri alphabetique, ce qui mettrait "annulation" avant "validation",
     * moins intuitif pour le narratif.
     */
    private static final Map<String, Map<String, String>> TRANSITIONS = buildTransitions();

    private static Map<String, Map<String, String>> buildTransitions() {
        Map<String, Map<String, String>> table = new LinkedHashMap<>();

        // Depuis "creation"
        Map<String, String> fromCreation = new LinkedHashMap<>();
        fromCreation.put(ACTION_VALIDATION, EN_ATTENTE_VALIDATION);
        fromCreation.put(ACTION_ANNULATION, ANNULEE);
        table.put(CREATION, fromCreation);

        // Depuis "en-attente-validation"
        Map<String, String> fromEnAttente = new LinkedHashMap<>();
        fromEnAttente.put(ACTION_VALIDATION, VALIDEE);
        fromEnAttente.put(ACTION_ANNULATION, ANNULEE);
        table.put(EN_ATTENTE_VALIDATION, fromEnAttente);

        // Depuis "validee"
        Map<String, String> fromValidee = new LinkedHashMap<>();
        fromValidee.put(ACTION_CONFIRMATION, CONFIRMEE);
        fromValidee.put(ACTION_ANNULATION, ANNULEE);
        table.put(VALIDEE, fromValidee);

        // Depuis "confirmee"
        Map<String, String> fromConfirmee = new LinkedHashMap<>();
        fromConfirmee.put(ACTION_TERMINAISON, TERMINEE);
        table.put(CONFIRMEE, fromConfirmee);

        // Etats terminaux : aucune transition possible
        // On les enregistre comme cles avec une map vide, ce qui simplifie
        // les tests (la cle existe, mais aucune action n'est valide)
        table.put(TERMINEE, Collections.emptyMap());
        table.put(ANNULEE, Collections.emptyMap());

        return Collections.unmodifiableMap(table);
    }

    /**
     * Resout l'etat cible d'une transition, ou retourne Optional.empty()
     * si la transition est invalide.
     *
     * Cas Optional.empty() :
     *   - L'etat courant n'est pas reconnu
     *   - L'action n'est pas autorisee depuis cet etat
     *   - L'etat courant est terminal (terminee ou annulee)
     *
     * Pourquoi Optional plutot qu'une exception ?
     *   - C'est le caller (le store ou le controller) qui sait quoi faire
     *     d'une transition invalide. Le workflow n'a pas a imposer une
     *     forme d'echec (exception, code retour, message).
     *   - Optional rend l'API explicite : impossible d'oublier de gerer
     *     le cas "transition invalide".
     *
     * @param fromState etat courant du document
     * @param action    action declenchante (ex: "validation")
     * @return Optional contenant l'etat cible si la transition est valide
     */
    public Optional<String> resolveTargetState(String fromState, String action) {
        if (fromState == null || action == null) {
            return Optional.empty();
        }
        Map<String, String> validActions = TRANSITIONS.get(fromState);
        if (validActions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(validActions.get(action));
    }

    /**
     * Verifie si une transition est valide, sans recuperer l'etat cible.
     * Sucre syntaxique pour le code qui n'a pas besoin de la cible.
     */
    public boolean canTransition(String fromState, String action) {
        return resolveTargetState(fromState, action).isPresent();
    }

    /**
     * Retourne l'ensemble des actions executables depuis un etat donne.
     *
     * Utilisee par DetailDocumentResponse pour remplir le champ
     * "action_possible" dans la reponse de detail-document.
     *
     * Retourne un Set vide si l'etat est inconnu ou terminal.
     * Retourne un TreeSet pour avoir un ordre stable et alphabetique
     * (les tests deviennent deterministes, et la reponse JSON est stable).
     *
     * @param fromState etat courant
     * @return ensemble (immuable) des actions valides
     */
    public Set<String> availableActions(String fromState) {
        if (fromState == null) {
            return Collections.emptySet();
        }
        Map<String, String> validActions = TRANSITIONS.get(fromState);
        if (validActions == null || validActions.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new TreeSet<>(validActions.keySet()));
    }
}