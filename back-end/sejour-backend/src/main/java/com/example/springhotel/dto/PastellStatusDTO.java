package com.example.springhotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Snapshot de l'etat de l'integration Pastell, retourne par
 * {@code GET /api/admin/pastell/status} (Lot 6).
 * <p>
 * Consomme par :
 *   <ul>
 *     <li>Le dashboard de demo (page {@code status.html}) qui poll cet endpoint
 *         toutes les 5s et l'affiche en clair pour un visiteur de portfolio.</li>
 *     <li>UptimeRobot et Render, qui peuvent aussi utiliser {@code /actuator/health}
 *         pour un check plus generique.</li>
 *   </ul>
 * <p>
 * Aucune information sensible (credentials, secrets) n'est exposee dans cette
 * reponse. Seuls les compteurs et les timestamps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PastellStatusDTO {

    /**
     * Horodatage de generation cote serveur. Permet a la page status.html
     * de detecter si elle voit une reponse en cache ou non.
     */
    private LocalDateTime generatedAt;

    /**
     * True si la propriete {@code pastell.enabled} vaut true cote backend.
     * Permet au dashboard d'afficher "integration desactivee" sans confusion.
     */
    private boolean pastellEnabled;

    /**
     * Valeur courante du curseur de polling Lot 5.
     * Reste a 0 tant qu'aucun evenement Pastell n'a ete traite.
     */
    private Long lastProcessedIdJ;

    /**
     * Horodatage du dernier polling reussi. Null si le scheduler n'a pas
     * encore tourne. Permet d'afficher "dernier contact il y a X secondes".
     */
    private LocalDateTime lastPolledAt;

    /** Nombre total de PastellSync dans chaque statut. */
    private long syncCountOk;
    private long syncCountPending;
    private long syncCountEnRetry;
    private long syncCountEnErreur;
    private long syncCountDivergence;

    /** Nombre total de reservations en base, tous statuts confondus. */
    private long reservationCount;

    /**
     * Resultat du ping HTTP vers le mock Pastell.
     * Indique si le mock est joignable depuis sejour-backend.
     */
    private MockHealth mockHealth;

    /**
     * Synthese du healthcheck cote mock.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MockHealth {
        /** True si l'appel a abouti avec un 2xx. */
        private boolean reachable;
        /** Temps de reponse en ms, ou null en cas d'echec. */
        private Long responseTimeMs;
        /** Message d'erreur si reachable=false, sinon null. */
        private String errorMessage;
    }
}
