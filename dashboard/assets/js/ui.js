/* ============================================================
 * ui.js - Rendu DOM et helpers d'affichage
 * ============================================================
 *
 * Toutes les fonctions qui MODIFIENT le DOM (texte, classes,
 * affichage/masquage) sont ici. Aucune logique metier, aucun
 * appel HTTP : juste de la presentation pure.
 *
 * Pourquoi cette separation ?
 *   - Permet de tester les fonctions de rendu sans appeler
 *     l'API reelle (on peut leur passer des donnees fakes).
 *   - Le code metier (api.js, main.js) reste lisible.
 *   - Si on voulait migrer vers React/Vue plus tard, c'est
 *     ici qu'on remplacerait par des composants.
 * ============================================================ */


/* ============================================================
 * HELPERS GENERIQUES
 * ============================================================ */


/**
 * Affiche un toast d'erreur en bas a droite, qui disparait
 * automatiquement apres 4 secondes.
 *
 * Pourquoi un toast plutot qu'une alert() ?
 *   - alert() bloque toute interaction le temps qu'elle soit
 *     fermee, c'est tres intrusif.
 *   - Un toast notifie sans interrompre le flow utilisateur.
 *
 * @param {string} message
 */
function showError(message) {
  const toast = document.getElementById('error-toast');
  toast.textContent = message;
  toast.style.display = 'block';
  setTimeout(() => { toast.style.display = 'none'; }, 4000);
}


/**
 * Genere une ligne HTML "cle: valeur" pour les panneaux.
 * Utilise dans les panneaux Sejour et Pastell pour afficher
 * proprement les attributs.
 *
 * @param {string} key - nom du champ (affiche en gris)
 * @param {string} valHtml - valeur (peut contenir du HTML)
 * @returns {string} HTML de la ligne
 */
function renderKvRow(key, valHtml) {
  return `
    <div class="kv-row">
      <span class="kv-key">${key}</span>
      <span class="kv-val">${valHtml}</span>
    </div>
  `;
}


/* ============================================================
 * COULEURS SEMANTIQUES
 * ============================================================ */


/**
 * Determine la classe CSS de couleur a appliquer pour un
 * statut de reservation Sejour.
 *
 * @param {string} statut - CONFIRMEE | TERMINEE | ANNULEE | EN_ATTENTE
 * @returns {string} nom de classe CSS
 */
function getStatusColor(statut) {
  if (!statut) return 'dim';
  const s = statut.toUpperCase();
  if (s === 'CONFIRMEE' || s === 'EN_ATTENTE') return 'blue';
  if (s === 'TERMINEE') return 'green';
  if (s === 'ANNULEE')  return 'red';
  return '';
}


/**
 * Determine la classe CSS de couleur pour une action Pastell
 * (sert a colorer les entrees du journal).
 *
 * @param {string} action
 * @returns {string} nom de classe CSS (ou chaine vide)
 */
function getPastellActionColor(action) {
  if (!action) return '';
  // Les classes correspondent exactement aux noms d'actions Pastell
  const knownActions = [
    'creation', 'en-attente-validation', 'validee',
    'confirmee', 'terminee', 'annulee'
  ];
  return knownActions.includes(action) ? action : '';
}


/* ============================================================
 * RENDU DU WORKFLOW (machine a etats)
 * ============================================================ */


/**
 * Met a jour visuellement le diagramme de workflow en haut
 * de la page : noeud actif allume, fleches passees allumees.
 *
 * @param {string} activeState - nom de l'etat courant (ex: "validee")
 */
function updateWorkflowDiagram(activeState) {
  const allNodes = [
    'creation', 'en-attente-validation', 'validee',
    'confirmee', 'terminee', 'annulee'
  ];

  // Etape 1 : eteindre tous les noeuds
  allNodes.forEach(name => {
    const el = document.getElementById('wf-' + name);
    if (el) el.classList.remove('active');
  });

  // Etape 2 : eteindre toutes les fleches
  [1, 2, 3, 4].forEach(i => {
    const arrow = document.getElementById('arr-' + i);
    if (arrow) arrow.classList.remove('active');
  });

  // Etape 3 : allumer le noeud actif
  const activeNode = document.getElementById('wf-' + activeState);
  if (activeNode) activeNode.classList.add('active');

  // Etape 4 : allumer les fleches deja parcourues
  // Ordre = creation -> en-attente -> validee -> confirmee -> terminee
  const order = ['creation', 'en-attente-validation', 'validee', 'confirmee', 'terminee'];
  const idx = order.indexOf(activeState);
  if (idx > 0) {
    for (let i = 1; i <= idx; i++) {
      const arrow = document.getElementById('arr-' + i);
      if (arrow) arrow.classList.add('active');
    }
  }
}


/* ============================================================
 * RENDU DU PANNEAU SEJOUR
 * ============================================================ */


/**
 * Affiche les details d'une reservation dans le panneau Sejour.
 *
 * @param {object} reservation - donnees brutes de l'API
 */
function renderReservation(reservation) {
  const colorClass = getStatusColor(reservation.statut);

  // Met a jour le badge de statut en haut du panneau
  const badge = document.getElementById('badge-resa');
  badge.className = 'badge ' + (
    colorClass === 'green' ? 'ok' :
    colorClass === 'red'   ? 'warn' : 'blue'
  );
  badge.textContent = reservation.statut || '—';

  // Construit le contenu detaille via les rows kv
  const body = document.getElementById('resa-body');
  body.innerHTML = [
    renderKvRow('id',                `<b>${reservation.id}</b>`),
    renderKvRow('nomClient',         reservation.nomClient || '—'),
    renderKvRow('emailClient',       reservation.emailClient || '—'),
    renderKvRow('chambre',           (reservation.chambreNom || '—') +
                                       (reservation.hotelNom ? ' · ' + reservation.hotelNom : '')),
    renderKvRow('dates',             (reservation.dateDebut || '—') + ' → ' + (reservation.dateFin || '—')),
    renderKvRow('prixTotal',         reservation.prixTotal !== undefined
                                       ? `<b>${reservation.prixTotal} €</b>` : '—'),
    renderKvRow('statut',            `<b class="${colorClass}">${reservation.statut || '—'}</b>`),
    renderKvRow('codeConfirmation',  reservation.codeConfirmation || '—'),
  ].join('');
}


/**
 * Affiche un message d'erreur dans le panneau Sejour.
 * @param {string} message
 */
function renderReservationError(message) {
  document.getElementById('resa-body').innerHTML =
    `<div class="empty-state" style="color:var(--warn)">${message}</div>`;
}


/**
 * Affiche un spinner de chargement dans le panneau Sejour.
 */
function renderReservationLoading() {
  document.getElementById('resa-body').innerHTML =
    '<div class="empty-state"><span class="spinner"></span> Chargement…</div>';
}


/* ============================================================
 * RENDU DU PANNEAU PASTELL
 * ============================================================ */


/**
 * Affiche les details d'un dossier Pastell.
 *
 * @param {object} document - reponse complete du mock (info + action_possible)
 * @param {string} idD - id du dossier (au cas ou info.id_d serait absent)
 */
function renderPastellDocument(document, idD) {
  const info = document.info || {};
  const colorClass = getPastellActionColor(info.last_action);

  // Met a jour le badge de l'etat courant
  const badge = document.getElementById('badge-pastell');
  badge.className = 'badge ' + (
    colorClass === 'terminee' ? 'ok' :
    colorClass === 'annulee'  ? 'warn' : 'blue'
  );
  badge.textContent = info.last_action || '—';

  // Construit le contenu du body
  const actionsList = (document.action_possible || []);
  const actionsHtml = actionsList.length > 0
    ? actionsList.map(a =>
        `<span style="color:var(--accent2);margin-right:8px">${a}</span>`
      ).join('')
    : '<span class="dim">aucune (état terminal)</span>';

  const body = window.document.getElementById('pastell-body');
  body.innerHTML = [
    renderKvRow('id_d',               `<b>${info.id_d || idD}</b>`),
    renderKvRow('id_e',               info.id_e || '—'),
    renderKvRow('type',               info.type || '—'),
    renderKvRow('last_action',        `<b class="${colorClass}">${info.last_action || '—'}</b>`),
    renderKvRow('last_action_date',   info.last_action_date || '—'),
    renderKvRow('actions possibles',  actionsHtml),
  ].join('');
}


function renderPastellLoading() {
  document.getElementById('pastell-body').innerHTML =
    '<div class="empty-state"><span class="spinner"></span> Chargement…</div>';
}


function renderPastellError(message) {
  document.getElementById('pastell-body').innerHTML =
    `<div class="empty-state" style="color:var(--warn)">${message}</div>`;
}


/* ============================================================
 * RENDU DES BOUTONS D'ACTION
 * ============================================================ */


/**
 * Genere dynamiquement les boutons d'action Pastell.
 * Les boutons sont grises (disabled) si l'action n'est pas
 * disponible depuis l'etat courant du dossier.
 *
 * @param {Array<string>} actionsPossibles - actions autorisees
 * @param {function} onActionClick - callback quand l'utilisateur clique
 */
function renderActionButtons(actionsPossibles, onActionClick) {
  const grid = document.getElementById('actions-grid');

  // Cas : pas de dossier charge -> message vide
  if (!actionsPossibles) {
    grid.innerHTML = '<div class="empty-state">Chargez un dossier Pastell pour voir les actions disponibles.</div>';
    return;
  }

  // Cas : etat terminal (plus aucune action possible)
  if (actionsPossibles.length === 0) {
    grid.innerHTML = `
      <div class="empty-state" style="color:var(--text-dim)">
        État terminal — aucune action possible dans Pastell.<br>
        Le polling Sejour va bientôt synchroniser cet état.
      </div>`;
    return;
  }

  // Cas normal : on rend tous les boutons (grises ou actifs selon dispo)
  const allActions = [
    { name: 'validation',   label: 'Validation',   description: 'Avance dans le workflow', danger: false },
    { name: 'confirmation', label: 'Confirmation', description: 'Valide la réservation',   danger: false },
    { name: 'terminaison',  label: 'Terminaison',  description: 'Clôture le dossier',      danger: false },
    { name: 'annulation',   label: 'Annulation',   description: 'Annule le dossier',       danger: true  },
  ];

  grid.innerHTML = ''; // On vide avant de repeupler

  allActions.forEach(action => {
    const enabled = actionsPossibles.includes(action.name);
    const button = window.document.createElement('button');
    button.className = 'action-btn' + (action.danger ? ' danger' : '');
    button.disabled = !enabled;
    button.innerHTML = `
      <span>${action.label}</span>
      <small>${enabled ? '→ ' + action.description : 'non disponible'}</small>
    `;
    if (enabled) {
      button.addEventListener('click', () => onActionClick(action.name));
    }
    grid.appendChild(button);
  });
}


/* ============================================================
 * RENDU DU JOURNAL
 * ============================================================ */


/**
 * Affiche les entrees du journal triees par id_j decroissant
 * (les plus recentes en haut).
 *
 * @param {Array<object>} entries - entrees brutes de l'API
 */
function renderJournal(entries) {
  const container = document.getElementById('journal-entries');

  if (!entries || entries.length === 0) {
    container.innerHTML = '<div class="empty-state">Journal vide.</div>';
    return;
  }

  // Tri antechronologique pour avoir le plus recent en haut
  const sorted = [...entries].sort((a, b) => b.id_j - a.id_j);

  container.innerHTML = sorted.map(entry => `
    <div class="journal-entry">
      <span class="j-idj">#${entry.id_j}</span>
      <span class="j-action ${entry.action || ''}">${entry.action || '—'}</span>
      <span class="j-idd">${(entry.id_d || '—').substring(0, 10)}…</span>
      <span class="j-date">${entry.date || ''}</span>
    </div>
  `).join('');
}


/* ============================================================
 * RENDU DE LA COHERENCE Sejour <-> Pastell
 * ============================================================ */


/**
 * Mapping des etats coherents : pour un statut Sejour donne,
 * quels etats Pastell sont compatibles ? Sert a detecter une
 * desynchronisation temporaire (Pastell est en avance, le
 * polling n'a pas encore tourne).
 */
const COHERENCE_MAP = {
  'CONFIRMEE':  ['creation', 'en-attente-validation', 'validee', 'confirmee'],
  'TERMINEE':   ['terminee'],
  'ANNULEE':    ['annulee'],
  'EN_ATTENTE': ['creation'],
};


/**
 * Affiche ou cache l'indicateur de coherence selon les
 * donnees disponibles dans state.
 */
function updateCoherenceIndicator() {
  const panel = document.getElementById('sync-coherence');
  const body = document.getElementById('coherence-body');
  const badge = document.getElementById('badge-coherence');

  // Cache si on n'a pas les deux cotes
  if (!state.resaStatut || !state.pastellAction) {
    panel.hidden = true;
    return;
  }

  panel.hidden = false;

  const allowedStates = COHERENCE_MAP[state.resaStatut] || [];
  const isAligned = allowedStates.includes(state.pastellAction);

  badge.className = 'badge ' + (isAligned ? 'ok' : 'warn');
  badge.textContent = isAligned ? 'Aligné' : 'Désynchronisé';

  body.innerHTML = `
    <div class="kv-row">
      <span class="kv-key">Statut Sejour</span>
      <span class="kv-val ${getStatusColor(state.resaStatut)}">${state.resaStatut}</span>
    </div>
    <div class="kv-row">
      <span class="kv-key">Dernier état Pastell</span>
      <span class="kv-val ${getPastellActionColor(state.pastellAction)}">${state.pastellAction}</span>
      ${isAligned
        ? '<span class="delta-tag delta-aligned">cohérent</span>'
        : '<span class="delta-tag delta-mismatch">⚠ pas encore synchronisé — prochain poll dans max 30s</span>'}
    </div>
  `;
}


/* ============================================================
 * EXPORT GLOBAL
 * ============================================================ */

window.ui = {
  showError,
  updateWorkflowDiagram,
  renderReservation,
  renderReservationLoading,
  renderReservationError,
  renderPastellDocument,
  renderPastellLoading,
  renderPastellError,
  renderActionButtons,
  renderJournal,
  updateCoherenceIndicator,
};