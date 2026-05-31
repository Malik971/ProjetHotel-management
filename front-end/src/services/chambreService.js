/**
 * chambreService.js
 * Service de gestion des chambres.
 *
 * Reecriture Lot 1 : utilise httpClient au lieu d'axios direct.
 *
 * NOMS DE METHODES : on expose les deux variantes pour etre compatible
 * avec tous les hooks et composants existants qui appellent ce service :
 *
 *   useChambres appelle getChambresByHotel (sans Id)
 *   useChambres appelle creerChambre (pas createChambre)
 *   D'autres composants peuvent appeler getChambresByHotelId (avec Id)
 *
 * Les deux noms sont disponibles, ils pointent sur la meme fonction.
 */

import { httpClient } from "../api/httpClient";

export async function getAllChambres() {
    const { data } = await httpClient.get("/api/chambres");
    return data;
}

/**
 * Recupere les chambres d'un hotel.
 * Deux noms exposes pour compatibilite : getChambresByHotel et getChambresByHotelId
 */
export async function getChambresByHotel(hotelId) {
    const { data } = await httpClient.get(`/api/chambres/hotel/${hotelId}`);
    return data;
}
// Alias pour les composants qui utilisent le nom long
export const getChambresByHotelId = getChambresByHotel;

export async function getChambreById(id) {
    const { data } = await httpClient.get(`/api/chambres/${id}`);
    return data;
}

export async function checkDisponibilite(chambreId, dateDebut, dateFin) {
    const { data } = await httpClient.get("/api/chambres/disponibilite", {
        params: { chambreId, dateDebut, dateFin },
    });
    return data;
}

/**
 * Cree une chambre (admin).
 * Deux noms exposes : creerChambre (useChambres) et createChambre (autre usage)
 */
export async function creerChambre(chambre) {
    const { data } = await httpClient.post("/api/chambres", chambre);
    return data;
}
export const createChambre = creerChambre;

export async function updateChambre(id, chambre) {
    const { data } = await httpClient.put(`/api/chambres/${id}`, chambre);
    return data;
}

export async function deleteChambre(id) {
    const { data } = await httpClient.delete(`/api/chambres/${id}`);
    return data;
}

/**
 * Objet groupant toutes les fonctions, pour les imports default ou nommes.
 * Inclut les deux variantes de noms pour garantir la retro-compatibilite.
 */
export const chambreService = {
    getAllChambres,
    getChambresByHotel,
    getChambresByHotelId,
    getChambreById,
    checkDisponibilite,
    creerChambre,
    createChambre,
    updateChambre,
    deleteChambre,
};

export default chambreService;