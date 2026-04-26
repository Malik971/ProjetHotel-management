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
 *   pastell.username=sejour
 *   pastell.password=${PASTELL_PASSWORD}
 *   pastell.entite-id=1
 *   pastell.type-dossier=reservation-hoteliere
 *   pastell.timeout-ms=5000
 *   pastell.webhook.enabled=false
 *   pastell.polling.interval-ms=30000
 * </pre>
 *
 * Validation :
 *   - Si {@code enabled=false} (defaut), aucune validation n'est appliquee : l'application
 *     demarre normalement sans Pastell configure.
 *   - Si {@code enabled=true}, tous les champs marques @NotBlank / @NotNull doivent etre
 *     renseignes, sinon l'application echoue au demarrage avec un message clair.
 *
 * DevRel note : la separation "enabled flag + validation conditionnelle" est un pattern
 * important pour une integration optionnelle. Un partenaire peut builder et lancer Sejour
 * sans avoir encore de Pastell, puis l'activer quand il est pret.
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
     *
     * Cette distinction n'a aucune consequence technique sur le client HTTP
     * (les deux parlent le meme protocole). C'est un marqueur documentaire
     * pour les logs de demarrage et l'observabilite.
     */
    @NotNull
    private Mode mode = Mode.MOCK;

    /**
     * URL de base de l'instance Pastell (sans slash final).
     * Exemple : http://localhost:8090
     * Les endpoints sont appeles sur {url}/api/{script}.php
     */
    private String url;

    /**
     * Login de l'utilisateur Pastell (authentification HTTP Basic).
     */
    private String username;

    /**
     * Mot de passe de l'utilisateur Pastell.
     * A ne JAMAIS commiter : doit venir d'une variable d'environnement ou d'un secret manager.
     * Exemple dans application.properties : pastell.password=${PASTELL_PASSWORD}
     */
    private String password;

    /**
     * Identifiant de l'entite (id_e) Pastell sur laquelle Sejour est autorise a operer.
     * Sejour etant mono-entite, cette valeur est fixe (typiquement 1 dans le mock).
     * Pour un deploiement multi-entites (ex. un office de tourisme par commune),
     * il faudrait resoudre cette valeur dynamiquement selon le contexte de la reservation.
     */
    private Long entiteId = 1L;

    /**
     * Nom du type de dossier a utiliser pour les reservations.
     * Doit correspondre a un type modelise dans Studio cote Pastell
     * (ou dans le YAML du mock au Lot 2).
     */
    @NotBlank
    private String typeDossier = "reservation-hoteliere";

    /**
     * Timeout des appels HTTP vers Pastell (connexion + lecture), en millisecondes.
     * Applique au RestClient dans PastellConfig.
     */
    @Min(500)
    private int timeoutMs = 5000;

    @Valid
    private Webhook webhook = new Webhook();

    @Valid
    private Polling polling = new Polling();

    /**
     * Configuration du webhook entrant Pastell -> Sejour (Lot 5).
     *
     * Pastell n'expose pas de webhooks natifs a ce jour. Cet endpoint est prevu
     * en anticipation d'une eventuelle evolution de l'API Pastell, ou pour un
     * scenario ou un middleware tiers declencherait des callbacks vers Sejour.
     */
    @Data
    public static class Webhook {
        /**
         * Quand false (defaut), l'endpoint /api/integration/pastell/webhook n'est pas
         * expose. Toute la synchronisation descendante passe par le polling.
         */
        private boolean enabled = false;
    }

    /**
     * Configuration du polling descendant Pastell -> Sejour (Lot 5).
     *
     * Un scheduler appelle journal.php a intervalle regulier pour detecter
     * les transitions declenchees cote Pastell qui n'ont pas ete initiees par Sejour.
     */
    @Data
    public static class Polling {
        /**
         * Intervalle entre deux appels a journal.php.
         * 30 secondes par defaut : compromis entre reactivite UX et charge Pastell.
         */
        @Min(1000)
        private long intervalMs = 30000L;
    }

    /**
     * Mode de fonctionnement de l'integration.
     * Cette enum est volontairement simple : elle ne modifie pas le comportement
     * du client HTTP, elle sert a raconter ce qu'on fait dans les logs et dashboards.
     */
    public enum Mode {
        /** Mock Pastell embarque (developpement, CI, demo). */
        MOCK,
        /** Instance Pastell reelle (Libriciel, production). */
        REAL
    }

    /**
     * Validation manuelle appelee depuis {@link PastellConfig} au demarrage,
     * uniquement si {@code enabled=true}.
     *
     * On ne met PAS @NotBlank sur url / username / password au niveau des champs,
     * parce qu'on veut pouvoir demarrer l'application sans les renseigner quand
     * l'integration est desactivee. La validation conditionnelle se fait ici.
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
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(
                    "pastell.enabled=true mais pastell.username est vide. " +
                            "Renseignez le login de l'utilisateur technique Pastell."
            );
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "pastell.enabled=true mais pastell.password est vide. " +
                            "Renseignez le mot de passe via une variable d'environnement : " +
                            "pastell.password=${PASTELL_PASSWORD}"
            );
        }
        if (entiteId == null || entiteId < 1) {
            throw new IllegalStateException(
                    "pastell.enabled=true mais pastell.entite-id est invalide (doit etre >= 1). " +
                            "Valeur courante : " + entiteId
            );
        }
    }
}