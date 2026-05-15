/**
 * hotelSearchService.js
 * Service de recherche d'hotels.
 *
 * Reecriture Lot 1 : utilise httpClient.
 *
 * Exports proposes (retro-compatibilite garantie) :
 *   - exports nommes par fonction
 *   - export default groupant tout
 *   - export nomme groupant tout
 */

import { httpClient } from "../api/httpClient";

/**
 * Recupere la liste de tous les hotels (catalogue public).
 */
export async function getAllHotels() {
    const { data } = await httpClient.get("/api/hotels");
    return data;
}

/**
 * Recupere le detail d'un hotel par son id.
 */
export async function getHotelById(id) {
    const { data } = await httpClient.get(`/api/hotels/${id}`);
    return data;
}

/**
 * Recherche d'hotels par criteres.
 *
 * @param {Object} criteria { ville, dateArrivee, dateDepart, voyageurs }
 */
export async function searchHotels(criteria) {
    const { data } = await httpClient.post("/api/hotels/search", criteria);
    return data;
}

export const hotelSearchService = {
    getAllHotels,
    getHotelById,
    searchHotels,
};

export default hotelSearchService;