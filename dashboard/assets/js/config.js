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
 * ============================================================ */


/**
 * Lit la valeur d'un champ de configuration et nettoie les
 * slashes en fin (sinon on aurait des URLs comme
 * "http://localhost:8080//api/..." qui peuvent causer des 404).
 *
 * @param {string} key - cle de la config ('sejour', 'mock', 'user', 'pass')
 * @returns {string} valeur du champ, sans slash final
 */
function readConfig(key) {
  const el = document.getElementById('cfg-' + key);
  if (!el) return '';
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
 *   config.sejour()        -> "http://localhost:8080"
 *   config.mock()          -> "http://localhost:8090"
 *   config.mockHeaders()   -> { Authorization: "Basic ..." }
 */
window.config = {
  sejour: () => readConfig('sejour'),
  mock: () => readConfig('mock'),
  mockHeaders: buildMockHeaders,
};