/**
 * slugify.js
 * Utilitaire de conversion nom d'hotel vers slug URL.
 *
 * Format du slug : {nom-en-kebab-case}-{id}
 * Exemple : "Hotel des Arceaux" + id 42 -> "hotel-des-arceaux-42"
 *
 * L'id en suffixe garantit l'unicite meme si deux hotels ont
 * un nom similaire. C'est la convention de l'industrie (Booking,
 * AirBnB, Amazon utilisent tous slug + id).
 */

/**
 * Convertit une chaine de caracteres en kebab-case sans accents.
 */
export function slugify(text) {
    if (!text) return "";
    return text
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[^a-z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "");
}

/**
 * Cree le slug complet d'un hotel : nom-kebab-{id}
 */
export function makeHotelSlug(hotel) {
    if (!hotel?.nom || !hotel?.id) return String(hotel?.id || "");
    return `${slugify(hotel.nom)}-${hotel.id}`;
}

/**
 * Extrait l'id numerique depuis un slug hotel.
 * "hotel-des-arceaux-42" → 42
 */
export function extractHotelIdFromSlug(slug) {
    if (!slug) return null;
    const parts = slug.split("-");
    const id = parseInt(parts[parts.length - 1], 10);
    return isNaN(id) ? null : id;
}