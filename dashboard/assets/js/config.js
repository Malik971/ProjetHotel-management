/* ============================================================
 * config.js - Lecture de la configuration utilisateur
 * ============================================================
 *
 * Centralise l'acces aux valeurs saisies dans la barre de
 * configuration (URLs, credentials du mock).
 *
 * Pourquoi un module separe ?
 *   - Si demain on veut sauvegarder la config dans localStorage
 *     ou la lire depuis une variable d'environnement, on ne
 *     touche QUE ce fichier.
 *   - Tous les autres modules appellent config.sejour() ou
 *     config.mockHeaders() sans savoir d'ou vient la valeur.
 *   - Principe de responsabilite unique : un module = un job.
 *
 * Ce module expose un objet global "config" via window.config
 * pour qu'il soit accessible depuis api.js, ui.js et main.js.
 *
 * Lot 6 : detection automatique de l'environnement.
 *   - En local (file:// ou localhost) : on garde les URLs telles
 *     que saisies dans les inputs (localhost par defaut).
 *   - En prod (deploye sur Netlify) : on substitue automatiquement
 *     par les URLs Render definies dans window.SPRINGHOTEL_CONFIG.
 *
 * Cette logique evite de devoir basculer manuellement les URLs
 * selon le contexte. L'utilisateur peut toujours surcharger
 * manuellement les inputs si necessaire.
 * ============================================================ */


/**
 * Indique si le dashboard tourne en local (developpement) ou
 * sur un serveur distant (prod Netlify).
 *
 * Critere : protocole file:// ou hostname qui contient "localhost"
 * ou "127.0.0.1". Tout le reste est considere comme prod.
 *
 * @returns {boolean} true si local, false si prod
 */
function isLocalEnvironment() {
  if (window.location.protocol === 'file:') return true;
  const hostname = window.location.hostname;
  return hostname === 'localhost'
      || hostname === '127.0.0.1'
      || hostname === '0.0.0.0'
      || hostname.endsWith('.local');
}


/**
 * Lit la valeur d'un champ de configuration et nettoie les
 * slashes en fin (sinon on aurait des URLs comme
 * "http://localhost:8080//api/..." qui peuvent causer des 404).
 *
 * En prod uniquement : si l'utilisateur n'a pas modifie l'input
 * (valeur par defaut localhost) et que window.SPRINGHOTEL_CONFIG
 * est defini, on substitue par l'URL Render. En local on garde
 * tel quel.
 *
 * @param {string} key - cle de la config ('sejour', 'mock', 'user', 'pass')
 * @returns {string} valeur du champ, sans slash final
 */
function readConfig(key) {
  const el = document.getElementById('cfg-' + key);
  if (!el) return '';

  // En prod, si l'input est encore sur localhost (pas modifie par l'utilisateur),
  // on bascule automatiquement sur l'URL Render correspondante.
  if (!isLocalEnvironment() && window.SPRINGHOTEL_CONFIG && el.value.includes('localhost')) {
    if (key === 'sejour' && window.SPRINGHOTEL_CONFIG.backendUrl) {
      el.value = window.SPRINGHOTEL_CONFIG.backendUrl;
    } else if (key === 'mock' && window.SPRINGHOTEL_CONFIG.mockUrl) {
      el.value = window.SPRINGHOTEL_CONFIG.mockUrl;
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
 * btoa() est une fonction native du navigateur qui encode en
 * base64. C'est l'equivalent de Base64.getEncoder() en Java.
 *
 * @returns {object} headers a passer a fetch()
 */
function buildMockHeaders() {
  const credentials = readConfig('user') + ':' + readConfig('pass');
  const encoded = btoa(credentials);
  return { 'Authorization': 'Basic ' + encoded };
}


/**
 * Objet expose globalement avec les fonctions d'acces a la config.
 * Les autres modules l'utilisent via :
 *   config.sejour()        -> "http://localhost:8080" en local, URL Render en prod
 *   config.mock()          -> "http://localhost:8090" en local, URL Render en prod
 *   config.mockHeaders()   -> { Authorization: "Basic ..." }
 */
window.config = {
  sejour: () => readConfig('sejour'),
  mock: () => readConfig('mock'),
  mockHeaders: buildMockHeaders,
  isLocal: isLocalEnvironment,
};