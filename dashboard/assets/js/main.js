/* ============================================================
 * main.js - Orchestration et evenements
 * ============================================================
 *
 * C'est le "chef d'orchestre" de l'app : il branche les
 * evenements DOM (clics, saisies) sur les fonctions metier
 * (api.js + ui.js).
 *
 * Aucune logique technique pure ici, juste de l'orchestration :
 *   "Quand on clique X, faire l'appel Y et afficher Z."
 *
 * Charge en dernier dans index.html (apres config.js,
 * state.js, api.js, ui.js).
 * ============================================================ */


/* ============================================================
 * ORCHESTRATIONS DE HAUT NIVEAU
 * ============================================================ */


/**
 * Charge une reservation : appelle Sejour, met a jour state,
 * declenche le rendu, et auto-charge le PastellSync associe.
 *
 * Cette fonction est appelee :
 *   - Quand l'utilisateur clique "Charger" sur le panneau Sejour
 *   - Quand un poll auto se declenche
 *   - Apres avoir execute une action Pastell (pour rafraichir)
 */
async function loadReservation() {
  const id = document.getElementById('inp-resa-id').value.trim();
  if (!id) return;

  state.resaId = id;
  ui.renderReservationLoading();

  try {
    const reservation = await api.fetchReservation(id);
    state.resaStatut = reservation.statut;
    ui.renderReservation(reservation);

    // Tente de decouvrir l'id_d Pastell associe
    await loadPastellSyncForReservation(id);
    ui.updateCoherenceIndicator();

  } catch (error) {
    ui.renderReservationError(error.message);
    ui.showError('Erreur réservation : ' + error.message);
  }
}


/**
 * Cherche le PastellSync d'une reservation et auto-remplit
 * le champ id_d s'il est trouve, puis charge le dossier Pastell.
 */
async function loadPastellSyncForReservation(reservationId) {
  const sync = await api.fetchPastellSync(reservationId);
  if (sync && sync.pastellDocumentId) {
    state.idD = sync.pastellDocumentId;
    document.getElementById('inp-idd').value = sync.pastellDocumentId;
    await loadPastellDocument();
  }
}


/**
 * Charge un dossier Pastell : appelle le mock, met a jour state,
 * met a jour le diagramme de workflow, regenere les boutons d'action.
 */
async function loadPastellDocument() {
  const idD = document.getElementById('inp-idd').value.trim();
  if (!idD) return;

  state.idD = idD;
  ui.renderPastellLoading();

  try {
    const doc = await api.fetchPastellDocument(idD);
    const info = doc.info || {};

    state.pastellAction = info.last_action;
    state.pastellActionsPossibles = doc.action_possible || [];

    ui.updateWorkflowDiagram(info.last_action);
    ui.renderPastellDocument(doc, idD);
    ui.renderActionButtons(state.pastellActionsPossibles, executeAction);
    ui.updateCoherenceIndicator();

  } catch (error) {
    ui.renderPastellError(error.message);
    ui.showError('Erreur Pastell mock : ' + error.message);
  }
}


/**
 * Execute une action sur le dossier Pastell courant.
 * Apres l'action, rafraichit immediatement les 3 panneaux
 * pour montrer le changement en direct.
 *
 * @param {string} actionName - "validation" | "confirmation" | etc.
 */
async function executeAction(actionName) {
  if (!state.idD) return;

  try {
    await api.executePastellAction(state.idD, actionName);
    // Rafraichit le panneau Pastell + le journal en parallele
    await Promise.all([loadPastellDocument(), loadJournal()]);
    // Puis recharge la reservation (le statut a peut-etre bouge si polling actif)
    if (state.resaId) await loadReservation();
  } catch (error) {
    ui.showError(`Action "${actionName}" refusée : ${error.message}`);
  }
}


/**
 * Charge le journal complet du mock Pastell (depuis id_j=0).
 */
async function loadJournal() {
  try {
    const entries = await api.fetchJournal(0);
    state.journalEntries = entries;
    ui.renderJournal(entries);
  } catch (error) {
    ui.showError('Journal non disponible : ' + error.message);
  }
}


/**
 * Rafraichit tous les panneaux d'un coup. Appele par le bouton
 * "Rafraichir tout" et par le poll auto.
 */
async function refreshAll() {
  await loadJournal();
  if (state.idD) await loadPastellDocument();
  if (state.resaId) await loadReservation();
  ui.updateCoherenceIndicator();
}


/* ============================================================
 * GESTION DU POLL AUTOMATIQUE
 * ============================================================
 *
 * Reproduit cote front ce que fait le scheduler Sejour cote
 * back : un appel toutes les 30 secondes pour voir les
 * changements. Permet de visualiser le polling en direct.
 * ============================================================ */


/**
 * Active ou desactive le poll automatique. Quand actif, un
 * compte a rebours d'1 seconde se decremente, et tous les
 * 30 ticks on rafraichit.
 */
function toggleAutoPoll() {
  state.pollEnabled = !state.pollEnabled;

  const button = document.getElementById('btn-poll-toggle');
  const status = document.getElementById('poll-status');
  const counter = document.getElementById('poll-counter');

  if (state.pollEnabled) {
    // Activation
    button.textContent = 'Arrêter poll auto';
    status.textContent = '● Poll auto : ON';
    status.classList.add('active');
    state.pollCountdown = 30;

    state.pollInterval = setInterval(async () => {
      state.pollCountdown--;
      counter.textContent = `Prochain refresh dans ${state.pollCountdown}s`;
      if (state.pollCountdown <= 0) {
        state.pollCountdown = 30;
        await refreshAll();
      }
    }, 1000);

  } else {
    // Desactivation
    button.textContent = 'Démarrer poll auto (30s)';
    status.textContent = '● Poll auto : OFF';
    status.classList.remove('active');
    counter.textContent = '';
    clearInterval(state.pollInterval);
    state.pollInterval = null;
  }
}


/* ============================================================
 * INITIALISATION : branchement des evenements DOM
 * ============================================================
 *
 * Quand la page est totalement chargee, on lie chaque bouton
 * et chaque input a sa fonction de gestion. Cette etape est
 * essentielle : sans ca, les boutons ne reagissent pas aux clics.
 * ============================================================ */


function init() {
  // Bouton "Rafraichir tout" en haut
  document.getElementById('btn-refresh-all')
    .addEventListener('click', refreshAll);

  // Boutons "Charger" sur les deux panneaux
  document.getElementById('btn-load-resa')
    .addEventListener('click', loadReservation);

  document.getElementById('btn-load-pastell')
    .addEventListener('click', loadPastellDocument);

  // Bouton de rafraichissement du journal
  document.getElementById('btn-refresh-journal')
    .addEventListener('click', loadJournal);

  // Boutons de polling auto
  document.getElementById('btn-poll-toggle')
    .addEventListener('click', toggleAutoPoll);

  document.getElementById('btn-poll-now')
    .addEventListener('click', refreshAll);

  // Au chargement de la page, on charge le journal pour avoir
  // un visuel immediat (meme si pas de reservation chargee)
  loadJournal();
}


// L'evenement DOMContentLoaded se declenche quand le HTML est
// totalement parse. C'est le moment ou tous les elements sont
// dispos pour qu'on attache nos listeners.
//
// Note : avec defer dans le script tag, ce n'est pas strictement
// necessaire, mais on le garde pour la robustesse.
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}