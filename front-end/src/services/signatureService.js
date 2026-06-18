/**
 * signatureService.js
 * Service centralise pour le workflow de signature electronique des reservations.
 *
 * Convention : chaque fonction extrait response.data avant de retourner,
 * pour etre coherente avec adminPastellService.js et les autres services.
 *
 * Point de migration niveau 3 : seules initierSignature et signerReservation
 * changeront de cible d'appel. Le reste (AdminReservationsEnAttente.jsx,
 * AdminSignaturePage.jsx) ne sera pas modifie.
 */

import { httpClient } from "../api/httpClient";

const BASE = "/api/admin/reservations";

/**
 * Recupere les reservations en attente de validation (EN_ATTENTE et SIGNATURE_EN_COURS).
 * @param {number} page  numero de page, 0-based
 * @param {number} size  taille de page
 */
export async function getReservationsEnAttente(page = 0, size = 20) {
    const { data } = await httpClient.get(`${BASE}/en-attente?page=${page}&size=${size}`);
    return data;
}

/**
 * Signale que l'admin a ouvert la page de signature.
 * Passe la reservation a SIGNATURE_EN_COURS.
 * @param {number} reservationId
 */
export async function initierSignature(reservationId) {
    const { data } = await httpClient.post(`${BASE}/${reservationId}/initier-signature`);
    return data;
}

/**
 * Apposer la signature sur le dossier.
 * Genere le PDF, passe le dossier a CONFIRMEE.
 *
 * @param {number} reservationId
 * @param {string} signatureBase64  image PNG du canvas en base64
 * @param {string} nomSignataire    nom et prenom de l'agent
 */
export async function signerReservation(reservationId, signatureBase64, nomSignataire) {
    const { data } = await httpClient.post(`${BASE}/${reservationId}/signer`, {
        signatureBase64,
        nomSignataire,
    });
    return data;
}

/**
 * Recupere le PDF signe en base64 pour telechargement.
 * @param {number} reservationId
 */
export async function getPdfReservation(reservationId) {
    const { data } = await httpClient.get(`${BASE}/${reservationId}/pdf`);
    return data;
}