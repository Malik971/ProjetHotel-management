/**
 * dashboard/status.js
 * --------------------------------------------------------------
 * Poll regulier de l'endpoint GET /api/admin/pastell/status et
 * rendu dans status.html.
 *
 * Structure :
 *   1. Lecture de window.SPRINGHOTEL_CONFIG (defini dans config.js).
 *      Detection auto local vs prod : en local on tape sur localhost,
 *      en prod sur les URLs Render.
 *   2. Boucle setInterval qui appelle fetchStatus() toutes les N ms.
 *   3. fetchStatus -> render() qui met a jour le DOM.
 *
 * Pas de framework, pas de dependance externe. JS vanilla.
 */

(() => {
  "use strict";

  /**
   * Detecte si on tourne en local (file://, localhost) ou en prod.
   */
  function isLocalEnvironment() {
    if (window.location.protocol === 'file:') return true;
    const hostname = window.location.hostname;
    return hostname === 'localhost'
        || hostname === '127.0.0.1'
        || hostname === '0.0.0.0'
        || hostname.endsWith('.local');
  }

  const config = window.SPRINGHOTEL_CONFIG || {};

  // Choix de l'URL backend selon l'environnement.
  // En local on tape sur localhost:8080 par defaut, en prod on prend la
  // valeur fournie dans window.SPRINGHOTEL_CONFIG.backendUrl.
  const BACKEND_URL = isLocalEnvironment()
      ? "http://localhost:8080"
      : (config.backendUrl || "");

  const REFRESH_MS = config.statusRefreshIntervalMs || 5000;
  const STATUS_ENDPOINT = BACKEND_URL + "/api/admin/pastell/status";

  // Refs DOM
  const $ = (id) => document.getElementById(id);
  const refs = {
    updatedAt: $("updated-at"),
    chipBackend: $("chip-backend"),
    detailBackend: $("detail-backend"),
    chipMock: $("chip-mock"),
    detailMock: $("detail-mock"),
    valuePolling: $("value-polling"),
    detailPolling: $("detail-polling"),
    countOk: $("count-ok"),
    countPending: $("count-pending"),
    countRetry: $("count-retry"),
    countErreur: $("count-erreur"),
    countDivergence: $("count-divergence"),
    countReservations: $("count-reservations"),
    rawJson: $("raw-json"),
    refreshSeconds: $("refresh-seconds"),
    sourceUrl: $("source-url"),
  };

  // Affichage initial du footer
  refs.refreshSeconds.textContent = Math.round(REFRESH_MS / 1000);
  refs.sourceUrl.textContent = STATUS_ENDPOINT;

  /**
   * Convertit un LocalDateTime ISO ("2026-05-11T14:32:01.123") en
   * "il y a X secondes / minutes" relatif a maintenant.
   */
  function timeAgo(isoString) {
    if (!isoString) return "jamais";
    const then = new Date(isoString);
    const diffMs = Date.now() - then.getTime();
    const seconds = Math.floor(diffMs / 1000);
    if (seconds < 60) return `il y a ${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `il y a ${minutes}min`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `il y a ${hours}h`;
    const days = Math.floor(hours / 24);
    return `il y a ${days}j`;
  }

  /**
   * Met a jour la pastille de couleur d'un chip selon un statut OK / NOK.
   */
  function setChipState(element, isOk, labelOk, labelNok) {
    element.className = "chip " + (isOk ? "chip-ok" : "chip-error");
    element.textContent = isOk ? labelOk : labelNok;
  }

  /**
   * Met le compteur principal en chip-error si > 0 (pour EN_RETRY, DIVERGENCE, EN_ERREUR).
   * Pour OK on garde vert, pour PENDING on garde bleu.
   */
  function renderCounter(element, value, baseClass) {
    element.textContent = value;
    element.className = "counter-value chip " + baseClass;
  }

  /**
   * Render principal : prend la reponse de /api/admin/pastell/status et
   * la projette sur le DOM.
   */
  function render(data) {
    // Backend joignable : oui par definition (on a recu la reponse).
    setChipState(refs.chipBackend, true, "en ligne", "indisponible");
    refs.detailBackend.textContent = `Pastell ${data.pastellEnabled ? "active" : "desactive"}`;

    // Mock health
    const mock = data.mockHealth || {};
    setChipState(refs.chipMock, mock.reachable, "en ligne", "indisponible");
    if (mock.reachable && mock.responseTimeMs != null) {
      refs.detailMock.textContent = `temps de reponse : ${mock.responseTimeMs} ms`;
    } else if (mock.errorMessage) {
      refs.detailMock.textContent = mock.errorMessage;
    } else {
      refs.detailMock.textContent = "";
    }

    // Polling
    refs.valuePolling.textContent = timeAgo(data.lastPolledAt);
    refs.detailPolling.textContent = `curseur id_j : ${data.lastProcessedIdJ ?? "-"}`;

    // Compteurs
    renderCounter(refs.countOk, data.syncCountOk ?? 0, "chip-ok");
    renderCounter(refs.countPending, data.syncCountPending ?? 0, "chip-pending");
    renderCounter(refs.countRetry, data.syncCountEnRetry ?? 0, "chip-retry");
    renderCounter(refs.countErreur, data.syncCountEnErreur ?? 0, "chip-error");
    renderCounter(refs.countDivergence, data.syncCountDivergence ?? 0, "chip-divergence");

    // Volumes
    refs.countReservations.textContent = data.reservationCount ?? 0;

    // JSON brut
    refs.rawJson.textContent = JSON.stringify(data, null, 2);

    // Timestamp
    const now = new Date();
    refs.updatedAt.textContent = `derniere maj : ${now.toLocaleTimeString("fr-FR")}`;
  }

  /**
   * Render en cas d'erreur reseau (backend down ou CORS, etc).
   * On retombe sur des etats neutres et un message explicite.
   */
  function renderError(err) {
    setChipState(refs.chipBackend, false, "en ligne", "indisponible");
    refs.detailBackend.textContent = err.message || "erreur de connexion";

    setChipState(refs.chipMock, false, "en ligne", "etat inconnu");
    refs.detailMock.textContent = "backend injoignable";

    refs.valuePolling.textContent = "?";
    refs.detailPolling.textContent = "donnees indisponibles";

    [refs.countOk, refs.countPending, refs.countRetry, refs.countErreur, refs.countDivergence]
      .forEach((el) => {
        el.textContent = "?";
        el.className = "counter-value chip chip-loading";
      });

    refs.countReservations.textContent = "?";
    refs.rawJson.textContent = "Erreur :\n" + (err.stack || err.message || String(err));

    refs.updatedAt.textContent = `derniere tentative : ${new Date().toLocaleTimeString("fr-FR")} (echec)`;
  }

  /**
   * Appelle l'endpoint status et passe la reponse au render.
   */
  async function fetchStatus() {
    try {
      const resp = await fetch(STATUS_ENDPOINT, {
        method: "GET",
        headers: { "Accept": "application/json" },
        // pas de credentials : l'endpoint est public en demo
      });
      if (!resp.ok) {
        throw new Error(`HTTP ${resp.status} ${resp.statusText}`);
      }
      const data = await resp.json();
      render(data);
    } catch (err) {
      console.error("fetchStatus failed :", err);
      renderError(err);
    }
  }

  // Premiere passe immediate, puis polling regulier.
  fetchStatus();
  setInterval(fetchStatus, REFRESH_MS);
})();