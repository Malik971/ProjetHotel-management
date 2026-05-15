/**
 * reservationService.js
 * Service de gestion des reservations.
 *
 * Reecriture Lot 1 : utilise httpClient. Le JWT est ajoute automatiquement
 * par l'interceptor, plus besoin de le gerer ici.
 *
 * Exports proposes (retro-compatibilite garantie) :
 *   - exports nommes par fonction : import { creerReservation } from '...'
 *   - export default groupant tout : import reservationService from '...'
 *   - export nomme groupant tout   : import { reservationService } from '...'
 *
 * Tu peux donc utiliser n'importe laquelle des syntaxes selon les composants.
 */

import { httpClient } from "../api/httpClient";

/**
 * Cree une nouvelle reservation. Le backend associe automatiquement
 * la reservation a l'utilisateur authentifie via le JWT, donc plus
 * besoin d'envoyer userId dans le body.
 */
export async function creerReservation(payload) {
    const { data } = await httpClient.post("/api/reservations", payload);
    return data;
}

/**
 * Recupere les reservations d'un utilisateur (lui-meme ou admin).
 */
export async function getReservationsByUser(userId) {
    const { data } = await httpClient.get(
        `/api/admin/reservations/user/${userId}`
    );
    return data;
}

/**
 * Recupere une reservation par son id.
 */
export async function getReservationById(id) {
    const { data } = await httpClient.get(`/api/admin/reservations/${id}`);
    return data;
}

/**
 * Annule une reservation.
 */
export async function annulerReservation(id) {
    const { data } = await httpClient.delete(`/api/admin/reservations/${id}`);
    return data;
}

/**
 * Recupere toutes les reservations (endpoint admin).
 */
export async function getAllReservations() {
    const { data } = await httpClient.get("/api/admin/reservations");
    return data;
}

/**
 * Objet groupant toutes les fonctions du service, pour les composants
 * qui preferent appeler via reservationService.creerReservation(...).
 */
export const reservationService = {
    creerReservation,
    getReservationsByUser,
    getReservationById,
    annulerReservation,
    getAllReservations,
};

export default reservationService;