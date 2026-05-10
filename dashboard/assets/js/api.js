/* ============================================================
 * api.js - Communication HTTP avec les backends
 * ============================================================
 *
 * Centralise TOUS les appels reseau de l'application :
 *   - Vers Sejour (sejour-backend, port 8080)
 *   - Vers le mock Pastell (pastell-mock, port 8090)
 *
 * Pourquoi un module separe pour l'API ?
 *   - Si l'API change (nouveau endpoint, nouveau format), on
 *     modifie ICI uniquement.
 *   - Le code UI reste lisible : "api.fetchReservation(1)"
 *     plutot que de balancer un fetch() dans une fonction
 *     qui devrait juste afficher.
 *   - Permet de mocker l'API en test si besoin.
 *
 * Toutes les fonctions sont async/await : elles renvoient
 * une Promise. L'appelant doit faire "await api.xxx()" ou
 * ".then(...)" pour recuperer le resultat.
 * ============================================================ */


/* ============================================================
 * APPELS VERS SEJOUR (sejour-backend)
 * ============================================================ */


/**
 * Recupere une reservation par son ID.
 *
 * Appelle l'endpoint admin (pas /api/client/...) parce que
 * ce dernier exige une authentification utilisateur que le
 * dashboard n'a pas. L'endpoint admin est en permitAll() dans
 * SecurityConfig pour faciliter la demo.
 *
 * @param {string|number} id - ID de la reservation
 * @returns {Promise<object>} la reservation
 * @throws {Error} si la reponse n'est pas 2xx
 */
async function fetchReservation(id) {
  const url = `${config.sejour()}/api/admin/reservations/${id}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}


/**
 * Recupere le PastellSync associe a une reservation.
 * Sert principalement a auto-decouvrir l'id_d Pastell pour
 * pre-remplir le champ d'input correspondant.
 *
 * Cette methode peut renvoyer null silencieusement si
 * l'endpoint n'existe pas (200 vs 404), pour ne pas bloquer
 * l'UI quand le PastellSync n'est pas encore cree.
 *
 * @param {string|number} reservationId
 * @returns {Promise<object|null>} le sync, ou null si introuvable
 */
async function fetchPastellSync(reservationId) {
  try {
    const url = `${config.sejour()}/api/admin/pastell-sync/reservation/${reservationId}`;
    const response = await fetch(url);
    if (!response.ok) return null;
    return await response.json();
  } catch {
    return null;       // Endpoint absent ou autre erreur reseau, on n'est pas bloquant
  }
}


/* ============================================================
 * APPELS VERS PASTELL-MOCK
 * ============================================================ */


/**
 * Recupere l'etat courant d'un dossier Pastell.
 *
 * Reponse attendue (extrait) :
 *   {
 *     "info": { "id_d": "...", "last_action": "creation" },
 *     "action_possible": ["validation", "annulation"]
 *   }
 *
 * @param {string} idD - identifiant Pastell du dossier
 * @returns {Promise<object>} la reponse complete du mock
 * @throws {Error} si HTTP non 2xx (ex: 404 si idD inexistant)
 */
async function fetchPastellDocument(idD) {
  const url = `${config.mock()}/api/v2/entite/1/document/${idD}`;
  const response = await fetch(url, { headers: config.mockHeaders() });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}


/**
 * Declenche une action sur un dossier Pastell (validation,
 * confirmation, terminaison, annulation).
 *
 * Le mock Pastell attend un POST en multipart/form-data
 * avec un champ "action". On utilise FormData() qui construit
 * automatiquement le bon format MIME.
 *
 * @param {string} idD - identifiant du dossier
 * @param {string} action - nom de l'action
 * @returns {Promise<object>} le nouvel etat du dossier apres l'action
 * @throws {Error} avec le message d'erreur du mock si action refusee
 */
async function executePastellAction(idD, action) {
  const formData = new FormData();
  formData.append('action', action);

  const url = `${config.mock()}/api/v2/entite/1/document/${idD}/action`;
  const response = await fetch(url, {
    method: 'POST',
    headers: config.mockHeaders(),
    body: formData,
  });

  if (!response.ok) {
    // Tente de lire le message d'erreur du mock pour l'afficher
    let errorMessage = `HTTP ${response.status}`;
    try {
      const errorBody = await response.json();
      errorMessage = errorBody.error_message || errorBody.message || errorMessage;
    } catch {
      // Body non-JSON ou vide, on garde le message par defaut
    }
    throw new Error(errorMessage);
  }

  return response.json();
}


/**
 * Recupere les entrees du journal Pastell depuis un id_j donne.
 *
 * C'est exactement l'endpoint que le scheduler de polling cote
 * Sejour appelle toutes les 30 secondes en backend. Le dashboard
 * fait la meme chose pour permettre un suivi en direct.
 *
 * @param {number} sinceIdJ - borne exclusive (default: 0 = tout le journal)
 * @returns {Promise<Array>} liste des entrees ordonnees par id_j
 * @throws {Error} si HTTP non 2xx
 */
async function fetchJournal(sinceIdJ = 0) {
  const url = `${config.mock()}/api/v2/journal?since_id_j=${sinceIdJ}`;
  const response = await fetch(url, { headers: config.mockHeaders() });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}


/* ============================================================
 * EXPORT GLOBAL
 * ============================================================
 *
 * On expose toutes les fonctions sous l'objet window.api pour
 * que les autres modules les utilisent comme :
 *   await api.fetchReservation(1)
 *   await api.executePastellAction(idD, 'validation')
 * ============================================================ */

window.api = {
  fetchReservation,
  fetchPastellSync,
  fetchPastellDocument,
  executePastellAction,
  fetchJournal,
};