// ============================================================
// useApi - Couche d'acces HTTP vers sejour-backend
// ============================================================
//
// Contrainte d'architecture : le dashboard ne contacte JAMAIS le connecteur
// (pastell-mock) en direct. Tous les appels passent par les endpoints admin de
// sejour-backend, qui portent les credentials Pastell cote serveur. Ce module
// centralise ces appels ; aucun composant ne fait de fetch en direct.
//
// Les operations qui font avancer le bus (poll, relance, action de demo) sont
// protegees par l'en-tete X-Demo-Token, lu depuis la configuration.
// ============================================================

import { useConfig } from "./useConfig";

/**
 * Construit le composable d'API. La base URL et le token sont lus dynamiquement
 * a chaque appel via la configuration reactive, ce qui permet de changer de
 * backend depuis le ConfigDrawer sans recharger la page.
 */
export function useApi() {
    const { config } = useConfig();

    /**
     * Retire un eventuel slash final de la base URL pour eviter les doubles
     * slashes lors de la concatenation.
     */
    function base() {
        return (config.backendUrl || "").replace(/\/+$/, "");
    }

    /**
     * Effectue un appel HTTP et renvoie le corps JSON. Leve une Error portant un
     * message lisible si la reponse n'est pas 2xx, en essayant d'extraire le
     * champ hint ou error renvoye par le backend.
     *
     * @param {string} path chemin relatif (commence par /api/...)
     * @param {object} options options fetch (method, headers, body)
     * @returns {Promise<any>} corps JSON parse, ou null si corps vide
     */
    async function request(path, options = {}) {
        const response = await fetch(base() + path, options);

        if (!response.ok) {
            let message = `HTTP ${response.status}`;
            try {
                const body = await response.json();
                message = body.hint || body.error || body.message || message;
            } catch {
                // Corps non JSON ou vide : on garde le message par defaut.
            }
            throw new Error(message);
        }

        // 204 No Content ou corps vide : on renvoie null sans tenter de parser.
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    }

    /**
     * Compose les en-tetes d'une operation protegee par X-Demo-Token.
     */
    function writeHeaders(extra = {}) {
        return {
            "X-Demo-Token": config.demoToken || "",
            ...extra,
        };
    }

    return {
        // ----- Lectures -----

        /** Page de dossiers (PastellSyncSummary) pour choisir un dossier de demo. */
        listSyncs(page = 0, size = 50) {
            return request(`/api/admin/pastell-sync?page=${page}&size=${size}`);
        },

        /** Reservation Sejour par identifiant. */
        getReservation(id) {
            return request(`/api/admin/reservations/${id}`);
        },

        /** Etat courant d'un dossier Pastell (etape circuit plus actions possibles). */
        getDemoDocument(idD) {
            return request(`/api/admin/pastell/demo/document/${encodeURIComponent(idD)}`);
        },

        /** Journal d'un dossier, tel que connu par Sejour (rempli par le polling). */
        getSyncJournal(syncId) {
            return request(`/api/admin/pastell-sync/${syncId}/journal`);
        },

        /** Compteurs de synchronisation plus sante du connecteur. */
        getStatus() {
            return request(`/api/admin/pastell/status`);
        },

        /** Curseur de polling (derniere entree de journal traitee). */
        getCursor() {
            return request(`/api/admin/pastell/cursor`);
        },

        // ----- Ecritures (X-Demo-Token) -----

        /** Fait avancer un dossier d'une etape circuit. */
        doDemoAction(idD, action) {
            return request(`/api/admin/pastell/demo/document/${encodeURIComponent(idD)}/action`, {
                method: "POST",
                headers: writeHeaders({ "Content-Type": "application/json" }),
                body: JSON.stringify({ action }),
            });
        },

        /** Force un tick de polling cote Sejour (comme le scheduler des 30s). */
        forcePoll() {
            return request(`/api/admin/pastell/poll`, {
                method: "POST",
                headers: writeHeaders(),
            });
        },

        /** Relance un dossier en anomalie. */
        retrySync(syncId) {
            return request(`/api/admin/pastell-sync/${syncId}/retry`, {
                method: "POST",
                headers: writeHeaders(),
            });
        },
    };
}
