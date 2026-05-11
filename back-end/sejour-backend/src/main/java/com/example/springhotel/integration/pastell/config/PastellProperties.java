package com.example.springhotel.integration.pastell.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration de l'integration Pastell.
 *
 * Mapping depuis application.properties (prefix "pastell.*"). Exemple :
 * <pre>
 *   pastell.enabled=true
 *   pastell.mode=MOCK
 *   pastell.url=http://localhost:8090
 *   pastell.username=sejour                    (mode statique uniquement)
 *   pastell.password=${PASTELL_PASSWORD}        (mode statique uniquement)
 *   pastell.master-secret=${PASTELL_MASTER_SECRET}  (mode rotatif Lot 6)
 *   pastell.entite-id=1
 *   pastell.type-dossier=reservation-hoteliere
 *   pastell.timeout-ms=5000
 *   pastell.webhook.enabled=false
 *   pastell.polling.enabled=true
 *   pastell.polling.interval-ms=30000
 * </pre>
 *
 * Validation :
 *   - Si {@code enabled=false} (defaut), aucune validation n'est appliquee : l'application
 *     demarre normalement sans Pastell configure.
 *   - Si {@code enabled=true}, soit {@code master-secret} doit etre renseigne (mode rotatif),
 *     soit {@code username}+{@code password} doivent l'etre (mode statique). Sinon l'application
 *     echoue au demarrage avec un message clair.
 *
 * DevRel note : la separation "enabled flag + validation conditionnelle" est un pattern
 * important pour une integration optionnelle. Un partenaire peut builder et lancer Sejour
 * sans avoir encore de Pastell, puis l'activer quand il est pret.
 *
 * Evolution Lot 6 : ajout du champ {@code masterSecret} pour activer la rotation
 * quotidienne des credentials via HMAC-SHA256. Voir {@code CREDENTIALS.md}.
 */
@ConfigurationProperties(prefix = "pastell")
@Validated
@Data
public class PastellProperties {

    /**
     * Active ou desactive totalement l'integration Pastell.
     * Quand false, aucun bean d'integration n'est charge (voir {@link PastellConfig}).
     */
    private boolean enabled = false;

    /**
     * Mode de fonctionnement.
     *   - MOCK : pointe vers le mock Spring Boot local (developpement, CI, demo)
     *   - REAL : pointe vers une instance Pastell reelle (Libriciel, production)
     */
    @NotNull
    private Mode mode = Mode.MOCK;

    /**
     * URL de base de l'instance Pastell (sans slash final).
     * Exemple : http://localhost:8090
     */
    private String url;

    /**
     * Login de l'utilisateur Pastell (mode statique uniquement).
     * En mode rotatif (master-secret defini), cette valeur est ignoree et le
     * username est derive du secret maitre.
     */
    private String username;

    /**
     * Mot de passe de l'utilisateur Pastell (mode statique uniquement).
     * En mode rotatif, cette valeur est ignoree et le password est derive
     * quotidiennement du secret maitre.
     */
    private String password;

    /**
     * Secret maitre pour la derivation HMAC des credentials (Lot 6).
     * Quand cette valeur est definie, elle prend le pas sur {@code username} et
     * {@code password} : le username devient stable et derive, le password tourne
     * quotidiennement (UTC).
     * <p>
     * Doit etre identique cote pastell-mock pour que l'authentification fonctionne.
     * Genere avec : {@code openssl rand -hex 32}. Ne JAMAIS commiter, injecter via
     * la variable d'environnement {@code PASTELL_MASTER_SECRET}.
     */
    private String masterSecret;

    /**
     * Identifiant de l'entite (id_e) Pastell sur laquelle Sejour est autorise a operer.
     */
    private Long entiteId = 1L;

    /**
     * Nom du type de dossier a utiliser pour les reservations.
     */
    @NotBlank
    private String typeDossier = "reservation-hoteliere";

    /**
     * Timeout des appels HTTP vers Pastell (connexion + lecture), en millisecondes.
     */
    @Min(500)
    private int timeoutMs = 5000;

    @Valid
    private Webhook webhook = new Webhook();

    @Valid
    private Polling polling = new Polling();

    @Valid
    private Retry retry = new Retry();

    @Data
    public static class Webhook {
        private boolean enabled = false;
    }

    @Data
    public static class Polling {
        private boolean enabled = true;

        @Min(1000)
        private long intervalMs = 30000L;
    }

    public enum Mode {
        MOCK,
        REAL
    }

    /**
     * Indique si le mode rotatif est actif.
     * Utilise par {@link PastellConfig} pour choisir entre l'interceptor d'auth
     * statique et l'interceptor d'auth rotatif.
     */
    public boolean isRotatingCredentialsEnabled() {
        return masterSecret != null && !masterSecret.isBlank();
    }

    /**
     * Validation manuelle appelee depuis {@link PastellConfig} au demarrage,
     * uniquement si {@code enabled=true}.
     *
     * Logique Lot 6 : si masterSecret est present, on n'exige plus username/password.
     * Si masterSecret est absent, on retombe sur la regle Lot 3 (username+password requis).
     *
     * @throws IllegalStateException avec un message clair si une valeur manque
     */
    public void validateIfEnabled() {
        if (!enabled) {
            return;
        }
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "pastell.enabled=true mais pastell.url est vide. " +
                            "Renseignez par exemple : pastell.url=http://localhost:8090"
            );
        }
        if (isRotatingCredentialsEnabled()) {
            // Mode rotatif : on a tout ce qu'il faut, username/password sont derives.
            // Aucune validation supplementaire de username/password.
        } else {
            // Mode statique : username+password requis.
            if (username == null || username.isBlank()) {
                throw new IllegalStateException(
                        "pastell.enabled=true mais ni pastell.master-secret ni pastell.username ne sont definis. " +
                                "Renseignez l'un des deux. " +
                                "Pour la prod, preferez : pastell.master-secret=${PASTELL_MASTER_SECRET}"
                );
            }
            if (password == null || password.isBlank()) {
                throw new IllegalStateException(
                        "pastell.enabled=true mais pastell.password est vide. " +
                                "Renseignez le mot de passe via une variable d'environnement, " +
                                "ou utilisez le mode rotatif via pastell.master-secret."
                );
            }
        }
        if (entiteId == null || entiteId < 1) {
            throw new IllegalStateException(
                    "pastell.enabled=true mais pastell.entite-id est invalide (doit etre >= 1). " +
                            "Valeur courante : " + entiteId
            );
        }
        if (retry.getMaxAttemptsImmediate() > retry.getMaxTentativesTotal()) {
            throw new IllegalStateException(
                    "pastell.retry.max-attempts-immediate (" + retry.getMaxAttemptsImmediate()
                            + ") ne peut pas depasser pastell.retry.max-tentatives-total ("
                            + retry.getMaxTentativesTotal() + "). "
                            + "Sinon le niveau 1 epuiserait deja le quota total des la premiere passe."
            );
        }
    }

    @Data
    public static class Retry {
        @Min(1)
        private int maxAttemptsImmediate = 3;

        @Min(0)
        private long initialDelayMs = 200L;

        @Min(1)
        private double multiplier = 2.0;

        @Min(0)
        private long maxDelayMs = 2000L;

        private boolean schedulerEnabled = true;

        @Min(1000)
        private long schedulerIntervalMs = 300_000L;

        @Min(1)
        private int schedulerBatchSize = 20;

        @Min(1)
        private int maxTentativesTotal = 10;
    }
}
