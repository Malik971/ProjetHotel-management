package com.example.springhotel.integration.pastell.entity;

/**
 * Statut technique de la synchronisation entre une Reservation locale
 * et son dossier Pastell associe.
 *
 * IMPORTANT : ce statut represente l'etat de la COMMUNICATION avec Pastell,
 * PAS l'etat metier de la reservation (qui reste dans {@link com.example.springhotel.entity.Reservation.StatutReservation}).
 *
 * Exemples :
 *   - Reservation CONFIRMEE + PastellSync OK             => tout va bien
 *   - Reservation CONFIRMEE + PastellSync EN_ERREUR      => Sejour a confirme, Pastell ne sait pas encore
 *   - Reservation CONFIRMEE + PastellSync DIVERGENCE     => Pastell dit "annulee", Sejour dit "confirmee", admin doit arbitrer
 */
public enum SyncStatus {

    /** Dernier echange avec Pastell a reussi, etat cote Pastell coherent avec Sejour. */
    OK,

    /** Le dernier appel Pastell a echoue de maniere definitive (apres retry). */
    EN_ERREUR,

    /** Une tentative a echoue, d'autres retries sont prevus. */
    EN_RETRY,

    /**
     * Pastell et Sejour ne sont pas d'accord sur l'etat du dossier.
     * Ce cas n'est jamais resolu automatiquement : Sejour reste autorite,
     * mais l'admin est notifie pour arbitrage via le dashboard (Lot 6).
     */
    DIVERGENCE
}