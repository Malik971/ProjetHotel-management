/* ============================================================
 * config.js - Lecture de la configuration utilisateur
 * ============================================================
 *
 * Centralise l'acces aux valeurs saisies dans la barre de
 * configuration (URLs, credentials du mock).
 *
 * Lot 6 : detection automatique de l'environnement.
 *   - En local (file:// ou localhost) : on garde les URLs telles
 *     que saisies dans les inputs (localhost par defaut).
 *   - En prod (deploye sur Netlify) : on substitue automatiquement
 *     par les URLs Render definies dans window.SPRINGHOTEL_CONFIG.
 *
 * Lot 6 + : persistance des valeurs entre rechargements via localStorage.
 *   - Au premier chargement : si localStorage contient des valeurs
 *     pour les inputs, on les restaure.
 *   - A chaque modification d'un input : on sauvegarde la nouvelle
 *     valeur dans localStorage.
 *   - L'utilisateur ne perd plus ses credentials du jour quand il
 *     navigue entre index.html et status.html, ni quand il fait F5.
 *
 * Stockage cle/valeur : prefixe "dashboard." pour eviter les
 * collisions avec d'autres apps qui pourraient partager le domaine.
 * ============================================================ */


/**
 * Indique si le dashboard tourne en local (developpement) ou
 * sur un serveur distant (prod Netlify).
 */
function isLocalEnvironment() {
  if (window.location.protocol === 'file:') return true;
  const hostname = window.location.hostname;
  return hostname === 'localhost'
      || hostname === '127.0.0.1'
      || hostname === '0.0.0.0'
      || hostname.endsWith('.local');
}


/* ============================================================
 * Persistance localStorage
 * ============================================================ */

/**
 * Prefixe applique a toutes les cles localStorage pour eviter
 * les collisions avec d'autres apps.
 */
const STORAGE_PREFIX = 'dashboard.';

/**
 * Liste des inputs a persister, avec leur cle localStorage.
 * Pour ajouter un nouveau champ, il suffit d'ajouter une entree ici.
 */
const PERSISTED_INPUTS = [
  { inputId: 'cfg-sejour',  storageKey: 'cfgSejour' },
  { inputId: 'cfg-mock',    storageKey: 'cfgMock' },
  { inputId: 'cfg-user',    storageKey: 'cfgUser' },
  { inputId: 'cfg-pass',    storageKey: 'cfgPass' },
  { inputId: 'inp-resa-id', storageKey: 'inpResaId' },
  { inputId: 'inp-idd',     storageKey: 'inpIdd' },
];

/**
 * Au chargement du DOM, lit localStorage et pre-remplit les inputs.
 * Si une cle n'existe pas en localStorage, on garde la valeur HTML
 * par defaut. Si l'input n'existe pas sur cette page (ex: status.html
 * n'a pas tous les inputs), on l'ignore silencieusement.
 */
function restoreInputsFromStorage() {
  PERSISTED_INPUTS.forEach(({ inputId, storageKey }) => {
    const el = document.getElementById(inputId);
    if (!el) return;
    const stored = localStorage.getItem(STORAGE_PREFIX + storageKey);
    if (stored !== null) {
      el.value = stored;
    }
  });
}

/**
 * Attache un listener 'input' a chaque champ persiste pour sauvegarder
 * la valeur a chaque frappe. C'est plus reactif que d'attendre un
 * 'change' qui ne se declenche qu'a la perte de focus.
 */
function attachStorageListeners() {
  PERSISTED_INPUTS.forEach(({ inputId, storageKey }) => {
    const el = document.getElementById(inputId);
    if (!el) return;
    el.addEventListener('input', () => {
      localStorage.setItem(STORAGE_PREFIX + storageKey, el.value);
    });
  });
}

// Initialisation : on attend que le DOM soit pret pour pouvoir
// trouver les inputs par id. Avec script defer en bas de body,
// document.readyState est en general deja 'complete' a ce stade,
// mais on garde la garde pour robustesse.
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    restoreInputsFromStorage();
    attachStorageListeners();
  });
} else {
  restoreInputsFromStorage();
  attachStorageListeners();
}


/* ============================================================
 * API publique (window.config)
 * ============================================================ */

/**
 * Lit la valeur d'un champ de configuration et nettoie les
 * slashes en fin.
 *
 * En prod uniquement : si l'input est encore sur localhost (pas modifie
 * par l'utilisateur) et que window.SPRINGHOTEL_CONFIG est defini, on
 * substitue par l'URL Render. En local on garde tel quel.
 *
 * @param {string} key - cle de la config ('sejour', 'mock', 'user', 'pass')
 * @returns {string} valeur du champ, sans slash final
 */
function readConfig(key) {
  const el = document.getElementById('cfg-' + key);
  if (!el) return '';

  if (!isLocalEnvironment() && window.SPRINGHOTEL_CONFIG && el.value.includes('localhost')) {
    if (key === 'sejour' && window.SPRINGHOTEL_CONFIG.backendUrl) {
      el.value = window.SPRINGHOTEL_CONFIG.backendUrl;
      // On synchronise localStorage avec la valeur prod auto-substituee,
      // sinon l'utilisateur verrait localhost reapparaitre apres un refresh.
      localStorage.setItem(STORAGE_PREFIX + 'cfgSejour', el.value);
    } else if (key === 'mock' && window.SPRINGHOTEL_CONFIG.mockUrl) {
      el.value = window.SPRINGHOTEL_CONFIG.mockUrl;
      localStorage.setItem(STORAGE_PREFIX + 'cfgMock', el.value);
    }
  }

  return el.value.replace(/\/$/, '');
}


/**
 * Construit le header HTTP Authorization pour l'authentification
 * Basic Auth sur le mock Pastell.
 *
 * Format : "Basic <base64(login:password)>"
 *
 * @returns {object} headers a passer a fetch()
 */
function buildMockHeaders() {
  const credentials = readConfig('user') + ':' + readConfig('pass');
  const encoded = btoa(credentials);
  return { 'Authorization': 'Basic ' + encoded };
}


/**
 * Vide les valeurs persistees en localStorage et recharge la page.
 * Utile pour un bouton "Reinitialiser la configuration" si on en
 * ajoute un un jour.
 */
function clearStoredConfig() {
  PERSISTED_INPUTS.forEach(({ storageKey }) => {
    localStorage.removeItem(STORAGE_PREFIX + storageKey);
  });
}


/**
 * Objet expose globalement avec les fonctions d'acces a la config.
 */
window.config = {
  sejour: () => readConfig('sejour'),
  mock: () => readConfig('mock'),
  mockHeaders: buildMockHeaders,
  isLocal: isLocalEnvironment,
  clearStored: clearStoredConfig,
};