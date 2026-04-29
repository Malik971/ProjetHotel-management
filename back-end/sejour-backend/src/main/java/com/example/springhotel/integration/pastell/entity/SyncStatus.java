package com.example.springhotel.integration.pastell.entity;

/**
 * Statut technique de la synchronisation entre une Reservation locale
 * et son dossier Pastell associe.
 *
 * IMPORTANT : ce statut represente l'etat de la COMMUNICATION avec Pastell,
 * PAS l'etat metier de la reservation (qui reste dans
 * {@link com.example.springhotel.entity.Reservation.StatutReservation}).
 *
 * Cycle de vie normal :
 * <pre>
 *   PENDING ──► OK
 *     │
 *     ▼
 *   EN_RETRY ──► OK       (le retry a fini par reussir)
 *     │
 *     ▼
 *   EN_ERREUR              (abandonne apres N tentatives, Lot 4)
 *
 *   OK ──► DIVERGENCE      (le polling Lot 5 detecte un ecart Pastell/Sejour)
 * </pre>
 *
 * Exemples concrets :
 *   - Reservation CONFIRMEE + PastellSync PENDING     =&gt; Pastell pas encore appele
 *   - Reservation CONFIRMEE + PastellSync OK          =&gt; tout va bien
 *   - Reservation CONFIRMEE + PastellSync EN_RETRY    =&gt; un appel a echoue, on va reessayer
 *   - Reservation CONFIRMEE + PastellSync EN_ERREUR   =&gt; Sejour a confirme, Pastell ne sait pas encore
 *   - Reservation CONFIRMEE + PastellSync DIVERGENCE  =&gt; Pastell dit "annulee", Sejour dit "confirmee"
 */
public enum SyncStatus {

    /**
     * Etat initial : la reservation a ete creee, un PastellSync est persiste
     * pour tracer l'intention de synchroniser, mais aucun appel HTTP n'a encore
     * ete tente vers Pastell. Le champ {@code pastellDocumentId} est null.
     *
     * Ce statut existe pour couvrir le cas ou le serveur crashe entre la creation
     * du PastellSync et l'appel create-document.php. Le job de reprise (Lot 4)
     * pourra retrouver les PENDING orphelins et les retraiter.
     */
    PENDING,

    /** Dernier echange avec Pastell a reussi, etat cote Pastell coherent avec Sejour. */
    OK,

    /**
     * Une tentative a echoue, d'autres retries sont prevus (Lot 4).
     * Le champ {@code pastellDocumentId} peut etre null si le tout premier
     * appel create-document.php a echoue avant de recevoir un id_d.
     */
    EN_RETRY,

    /** Le dernier appel Pastell a echoue de maniere definitive (apres tous les retries du Lot 4). */
    EN_ERREUR,

    /**
     * Pastell et Sejour ne sont pas d'accord sur l'etat du dossier.
     * Ce cas n'est jamais resolu automatiquement : Sejour reste autorite,
     * mais l'admin est notifie pour arbitrage via le dashboard (Lot 6).
     */
    DIVERGENCE
}