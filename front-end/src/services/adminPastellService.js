/**
 * adminPastellService.js
 * Service centralise pour tous les appels API du module admin Pastell.
 *
 * Le X-Demo-Token est ajoute automatiquement sur les operations destructives
 * (force poll, retry). Sa valeur est lue depuis VITE_DEMO_ADMIN_TOKEN.
 * En local sans token, le backend accepte tout.
 */

import { httpClient } from "../api/httpClient";

const DEMO_TOKEN = import.meta.env.VITE_DEMO_ADMIN_TOKEN || "";

function adminHeaders() {
    return DEMO_TOKEN ? { "X-Demo-Token": DEMO_TOKEN } : {};
}

/**
 * Snapshot global du bus Pastell (compteurs, curseur, ping mock).
 */
export async function getPastellStatus() {
    const { data } = await httpClient.get("/api/admin/pastell/status");
    return data;
}

/**
 * Liste paginee des dossiers (PastellSync).
 */
export async function listSyncs({ status, page = 0, size = 20 } = {}) {
    const params = { page, size };
    if (status) params.status = status;
    const { data } = await httpClient.get("/api/admin/pastell-sync", { params });
    return data;
}

/**
 * Detail complet d'une reservation cote admin.
 */
export async function getReservation(reservationId) {
    const { data } = await httpClient.get(
        `/api/admin/reservations/${reservationId}`
    );
    return data;
}

/**
 * PastellSync associe a une reservation.
 */
export async function getSyncByReservation(reservationId) {
    const { data } = await httpClient.get(
        `/api/admin/pastell-sync/reservation/${reservationId}`
    );
    return data;
}

/**
 * Journal d'orchestration d'un dossier.
 */
export async function getSyncJournal(syncId) {
    const { data } = await httpClient.get(
        `/api/admin/pastell-sync/${syncId}/journal`
    );
    return data;
}

/**
 * Activite recente du bus (pour le dashboard).
 */
export async function getRecentActivity(limit = 10) {
    const { data } = await httpClient.get("/api/admin/activity", {
        params: { limit },
    });
    return data;
}

/**
 * Force un tick de polling global. Destructif, exige X-Demo-Token.
 */
export async function forceGlobalPoll() {
    const { data } = await httpClient.post(
        "/api/admin/pastell/poll",
        {},
        { headers: adminHeaders() }
    );
    return data;
}

/**
 * Relance manuellement un sync specifique. Destructif, exige X-Demo-Token.
 */
export async function retrySync(syncId) {
    const { data } = await httpClient.post(
        `/api/admin/pastell-sync/${syncId}/retry`,
        {},
        { headers: adminHeaders() }
    );
    return data;
}

export default {
    getPastellStatus,
    listSyncs,
    getReservation,
    getSyncByReservation,
    getSyncJournal,
    getRecentActivity,
    forceGlobalPoll,
    retrySync,
};