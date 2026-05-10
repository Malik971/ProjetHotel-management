/* ============================================================
 * state.js - Etat applicatif global
 * ============================================================
 *
 * Centralise toutes les donnees "vivantes" de l'application :
 * id de la reservation chargee, id_d du dossier Pastell, etat
 * du polling, dernier journal recu.
 *
 * Pourquoi un objet d'etat partage ?
 *   - Plusieurs modules ont besoin de lire et modifier ces
 *     valeurs (api.js declenche des fetchs avec resaId, ui.js
 *     met a jour l'affichage en fonction de pastellAction, etc.)
 *   - Sans etat partage, il faudrait passer ces valeurs en
 *     parametres a toutes les fonctions, ce qui devient vite
 *     illisible.
 *
 * Pour des apps plus grosses, on utiliserait Redux, Zustand,
 * Pinia, etc. Ici on reste sur du vanilla : un simple objet
 * mutable. C'est volontaire pour rester pedagogique.
 * ============================================================ */


window.state = {

  /* ====== Identifiants charges ====== */

  /**
   * ID de la reservation actuellement chargee cote Sejour.
   * Null tant que l'utilisateur n'a pas clique "Charger".
   */
  resaId: null,

  /**
   * id_d du dossier Pastell associe a la reservation.
   * Auto-rempli depuis le PastellSync, ou saisi manuellement.
   */
  idD: null,


  /* ====== Etats observes ====== */

  /**
   * Statut courant de la reservation Sejour
   * (CONFIRMEE, TERMINEE, ANNULEE, EN_ATTENTE).
   */
  resaStatut: null,

  /**
   * Action courante du dossier Pastell
   * (creation, en-attente-validation, validee, confirmee,
   * terminee, annulee).
   */
  pastellAction: null,

  /**
   * Liste des actions encore possibles depuis l'etat courant
   * (ex: ["validation", "annulation"]). Utilisee pour griser
   * les boutons d'action non disponibles.
   */
  pastellActionsPossibles: [],


  /* ====== Polling automatique ====== */

  /**
   * Timer JavaScript du compte a rebours (1 tick par seconde).
   * Stocke pour pouvoir l'arreter avec clearInterval().
   */
  pollInterval: null,

  /**
   * Flag indiquant si le poll auto est actif.
   */
  pollEnabled: false,

  /**
   * Compte a rebours en secondes avant le prochain refresh.
   * Decremente chaque seconde quand pollEnabled est true.
   */
  pollCountdown: 30,


  /* ====== Donnees mises en cache ====== */

  /**
   * Dernier journal recu depuis le mock Pastell.
   * Utilise pour reafficher sans refaire l'appel HTTP.
   */
  journalEntries: [],
};