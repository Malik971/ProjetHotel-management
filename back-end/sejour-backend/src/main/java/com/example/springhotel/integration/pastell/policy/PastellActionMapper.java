package com.example.springhotel.integration.pastell.policy;

import com.example.springhotel.entity.Reservation.StatutReservation;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Table de decision : action Pastell -> intention metier cote Sejour.
 *<p>
 * Le polling descendant lit le journal Pastell et recoit des entrees du type
 * "le dossier abc-123 est passe en etat 'annulee' a 15h30". Il faut decider
 * ce que cette information change cote Sejour. Cette decision a trois aspects :
 *<p>
 *   1) Faut-il toucher au statut de la Reservation ? Si oui, quel statut cible ?
 *   2) Si Sejour est dans un statut incompatible avec l'action recue, est-ce
 *      une divergence (l'integration est desynchronisee, l'admin doit arbitrer) ?
 *   3) Si Sejour est deja dans le bon statut, on saute (mais on rafraichit
 *      pastell_etat_dernier_connu, traitement fait par le service appelant).
 *<p>
 * Cette classe encapsule UNIQUEMENT le point 1 (et fournit un helper pour le 2).
 * Le service appelant (PastellInboundSyncService) orchestre les trois aspects.
 *<p>
 * Pourquoi un composant Spring et pas une enum statique ?
 *   - Coherent avec PastellRetryPolicy (meme philosophie : table de decision
 *     pure, sans etat, injectable).
 *   - Permet de mocker la policy dans les tests pour simuler des comportements
 *     alternatifs (ex. tester le service sans toucher au mapping reel).
 *   - L'instance est sans etat, scope singleton de Spring sans probleme.
 *<p>
 * Decisions de mapping :
 *<p>
 *   "creation"
 *     -> AUCUNE bascule. C'est nous qui avons cree le dossier au Lot 3, on
 *        connait deja. Mappe a TRACE_ONLY (le service rafraichit juste l'etat
 *        connu sans toucher au statut).
 *<p>
 *   "en-attente-validation"
 *     -> AUCUNE bascule. Etat intermediaire cote Pastell. Sejour reste dans
 *        son statut courant (typiquement CONFIRMEE par defaut depuis Lot 3).
 *<p>
 *   "validee"
 *     -> AUCUNE bascule. Sejour est deja CONFIRMEE par defaut, et la "validation"
 *        cote Pastell n'est pas une confirmation cote metier hotelier. C'est juste
 *        une etape administrative cote Pastell.
 *<p>
 *   "confirmee"
 *     -> AUCUNE bascule. Idem, Sejour est deja CONFIRMEE.
 *<p>
 *   "terminee"
 *     -> Bascule TERMINEE. Choix Lot 5 (option A) : Pastell pilote la fin de
 *        vie du dossier. L'agent qui clique "terminer" cote Pastell fait passer
 *        la reservation Sejour en TERMINEE 30s plus tard via le polling.
 *<p>
 *   "annulee"
 *     -> Bascule ANNULEE. L'agent qui annule un dossier cote Pastell fait
 *        passer la reservation Sejour en ANNULEE.
 *<p>
 *   action inconnue (ex. "modifie", "transmise", future evolution Pastell)
 *     -> Optional.empty() : pas de changement de statut, le service decide quoi
 *        faire (typiquement : tracer dans les logs et avancer le curseur).
 *<p>
 * Pourquoi un Optional<StatutReservation> et pas une enum dediee ?
 *   - L'absence de valeur (Optional.empty) modelise naturellement les cas
 *     "ne touche pas au statut" et "action inconnue" en une seule reponse.
 *   - Le service appelant n'a pas a connaitre la liste des actions, il regarde
 *     juste si y'a une cible ou non.
 *   - Garde l'API minimaliste : pas besoin d'inventer une enum TRACE_ONLY /
 *     IGNORE / SET_STATUS quand un Optional fait le travail.
 *<p>
 * Si plus tard on voulait basculer en option B ("Sejour pilote tout seul la
 * fin de vie via un job interne") :
 *   - Il suffirait de retirer le case "terminee" -> TERMINEE ci-dessous, et
 *     l'action "terminee" tomberait dans le "default", ce qui revient a "ne
 *     touche pas au statut, juste tracer". Reversible sans toucher au reste.
 */
@Component
public class PastellActionMapper {

    // ====== Constantes des actions Pastell ======
    // On reprend les valeurs litterales du workflow modelise dans le mock
    // (DocumentTransitions cote pastell-mock). On NE depend PAS de la classe
    // DocumentTransitions du mock parce qu'on ne veut pas que sejour-backend
    // ait une dependance Maven sur pastell-mock (ce sont deux modules separes,
    // sejour-backend doit pouvoir parler a un vrai Pastell sans le mock).

    public static final String ACTION_CREATION = "creation";
    public static final String ACTION_EN_ATTENTE_VALIDATION = "en-attente-validation";
    public static final String ACTION_VALIDEE = "validee";
    public static final String ACTION_CONFIRMEE = "confirmee";
    public static final String ACTION_TERMINEE = "terminee";
    public static final String ACTION_ANNULEE = "annulee";

    /**
     * Decide quel StatutReservation cible appliquer pour une action Pastell donnee.
     *
     * @param action chaine recue dans le champ "action" d'une PastellJournalEntry,
     *               telle que renvoyee par Pastell ou le mock. Insensible a la casse.
     * @return Optional avec le statut cible si une bascule est requise, Optional.empty
     *         si aucun changement de statut n'est attendu (action neutre, inconnue, ou null).
     */
    public Optional<StatutReservation> resolveTargetStatus(String action) {
        if (action == null) {
            return Optional.empty();
        }
        String normalized = action.trim().toLowerCase();

        return switch (normalized) {
            case ACTION_TERMINEE -> Optional.of(StatutReservation.TERMINEE);
            case ACTION_ANNULEE -> Optional.of(StatutReservation.ANNULEE);
            // Toutes les autres actions sont neutres pour le statut metier :
            // creation, en-attente-validation, validee, confirmee, et les
            // actions futures que Pastell pourrait introduire.
            default -> Optional.empty();
        };
    }

    /**
     * Detecte si l'action Pastell entre en CONFLIT avec le statut courant
     * de la reservation Sejour. Un conflit signale une DIVERGENCE :
     * Pastell et Sejour ne sont pas d'accord sur le sort de la reservation.
     *<p>
     * Regles :
     *<p>
     *   - "annulee" cote Pastell + Sejour TERMINEE
     *     => DIVERGENCE. Une reservation deja terminee ne peut pas etre
     *        retroactivement annulee. C'est une incoherence metier.
     *<p>
     *   - "terminee" cote Pastell + Sejour ANNULEE
     *     => DIVERGENCE. Une reservation annulee ne peut pas avoir ete
     *        consommee jusqu'a son terme.
     *<p>
     *   - Tous les autres cas
     *     => pas de conflit. Si Sejour est deja dans le statut cible, c'est
     *        un alignement, pas une divergence. Si l'action est neutre
     *        (creation, validee...), pas d'opinion sur le statut, donc pas
     *        de conflit possible.
     *<p>
     * Pourquoi cette methode ici plutot que dans le service ?
     *   - C'est une regle de cohabitation des etats Pastell <-> Sejour, donc une
     *     regle metier au sens "transcription de la realite operationnelle".
     *     Le mapper est le bon endroit pour la garder.
     *   - Le service appelant a une responsabilite d'orchestration (lire, appliquer,
     *     persister) qui ne doit pas etre polluee par les regles de divergence.
     *
     * @param action       action Pastell recue (ex. "annulee")
     * @param statutCourant statut courant de la reservation Sejour
     * @return true si on detecte une incoherence, false sinon
     */
    public boolean isConflict(String action, StatutReservation statutCourant) {
        if (action == null || statutCourant == null) {
            return false;
        }
        String normalized = action.trim().toLowerCase();

        // Pastell veut annuler une reservation deja consommee : conflit.
        if (ACTION_ANNULEE.equals(normalized) && statutCourant == StatutReservation.TERMINEE) {
            return true;
        }
        // Pastell veut terminer une reservation deja annulee : conflit.
        if (ACTION_TERMINEE.equals(normalized) && statutCourant == StatutReservation.ANNULEE) {
            return true;
        }
        return false;
    }
}