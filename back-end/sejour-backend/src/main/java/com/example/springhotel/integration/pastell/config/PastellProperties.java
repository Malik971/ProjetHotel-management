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
 *   pastell.polling.enabled=true
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

    @Valid
    private Retry retry = new Retry();

    /**
     * Configuration du webhook entrant Pastell -> Sejour.
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
     *<p>
     * Un scheduler appelle GET /api/v2/journal a intervalle regulier pour detecter
     * les transitions declenchees cote Pastell qui n'ont pas ete initiees par Sejour
     * (typiquement : un agent qui valide ou annule un dossier directement dans
     * l'interface Pastell).
     */
    @Data
    public static class Polling {

        /**
         * Active ou non le scheduler de polling descendant (Lot 5).
         *<p>
         * Symetrique de {@code retry.scheduler-enabled} : permet de desactiver
         * finement le polling sans toucher a {@code pastell.enabled}. Utile :
         *   - En local quand on travaille sur la sync montante et qu'on ne
         *     veut pas etre derange par les logs du polling.
         *   - Dans certains tests ou scenarios CI ou on veut tester la sync
         *     montante seule.
         *<p>
         * Defaut a true : si l'integration Pastell est activee, le polling est
         * actif par defaut, ce qui est le comportement attendu en production.
         */
        private boolean enabled = true;

        /**
         * Intervalle entre deux appels a GET /api/v2/journal.
         * 30 secondes par defaut : compromis entre reactivite UX et charge Pastell.
         * Le minimum de 1000 ms evite les configurations hostiles qui taperaient
         * Pastell plusieurs fois par seconde par erreur.
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
        // Ajout Lot 4 : coherence entre maxAttemptsImmediate et maxTentativesTotal
        if (retry.getMaxAttemptsImmediate() > retry.getMaxTentativesTotal()) {
            throw new IllegalStateException(
                    "pastell.retry.max-attempts-immediate (" + retry.getMaxAttemptsImmediate()
                            + ") ne peut pas depasser pastell.retry.max-tentatives-total ("
                            + retry.getMaxTentativesTotal() + "). "
                            + "Sinon le niveau 1 epuiserait deja le quota total des la premiere passe."
            );
        }
    }

    /**
     * Configuration du retry des appels sortants Pastell (Lot 4).
     *<p>
     * Deux niveaux de retry coexistent et utilisent la meme configuration :
     *<p>
     *   Niveau 1 - retry court immediat (RetryTemplate dans PastellClientWithRetry) :
     *     Quand l'appel Pastell echoue, on retente immediatement avec un backoff
     *     exponentiel. Concretement : echec -> attendre {@code initialDelayMs},
     *     re-essayer. Echec encore -> attendre {@code initialDelayMs * multiplier},
     *     re-essayer. Etc. jusqu'a {@code maxAttemptsImmediate} tentatives au total
     *     ou {@code maxDelayMs} comme plafond entre deux tentatives.
     *     Ce niveau absorbe les hoquets reseau et les 5xx fugaces.
     *<p>
     *   Niveau 2 - reprise differee (PastellRetryScheduler) :
     *     Si malgre le niveau 1 la synchro echoue (ex. Pastell down depuis longtemps),
     *     le PastellSync est persiste en EN_RETRY. Toutes les
     *     {@code schedulerIntervalMs}, le scheduler reprend les EN_RETRY (et
     *     les PENDING orphelins) en lots de {@code schedulerBatchSize}, du plus
     *     ancien au plus recent (FIFO). Au-dela de {@code maxTentativesTotal}
     *     tentatives totales, le sync bascule en EN_ERREUR definitif.
     *<p>
     * Note DevRel : un partenaire qui adopte cette integration peut tuner les delais
     * sans recompiler. Toutes ces valeurs sont surchargeables via application.properties
     * ou variables d'environnement (PASTELL_RETRY_*).
     */
    @Data
    public static class Retry {

        /**
         * Nombre maximum de tentatives pour le retry court (niveau 1).
         * Inclut la tentative initiale : {@code maxAttemptsImmediate=3} signifie
         * 1 tentative + 2 retries au maximum.
         */
        @Min(1)
        private int maxAttemptsImmediate = 3;

        /**
         * Delai avant le PREMIER retry du niveau 1, en millisecondes.
         * Le delai croit ensuite par {@code multiplier} a chaque echec.
         */
        @Min(0)
        private long initialDelayMs = 200L;

        /**
         * Facteur multiplicatif du backoff exponentiel.
         * Avec multiplier=2.0 et initialDelayMs=200 : 200ms, 400ms, 800ms, 1600ms...
         * (jusqu'au plafond {@code maxDelayMs}).
         */
        @Min(1)
        private double multiplier = 2.0;

        /**
         * Plafond du delai entre deux tentatives (niveau 1), en millisecondes.
         * Empeche que le backoff explose si on configure un grand nombre de tentatives.
         */
        @Min(0)
        private long maxDelayMs = 2000L;

        /**
         * Active ou non le scheduler de reprise (niveau 2).
         * Pratique pour le desactiver en local (developpement) tout en gardant
         * pastell.enabled=true.
         */
        private boolean schedulerEnabled = true;

        /**
         * Intervalle entre deux passes du scheduler de reprise, en millisecondes.
         * Defaut : 5 minutes. Compromis entre reactivite (rattraper rapidement
         * une panne resolue) et charge (ne pas spammer Pastell quand il y a
         * beaucoup de syncs en attente).
         */
        @Min(1000)
        private long schedulerIntervalMs = 300_000L;

        /**
         * Nombre maximum de syncs traites par passe du scheduler.
         * Empeche le scheduler de noyer Pastell apres une panne longue
         * qui aurait laisse des centaines de syncs en attente.
         */
        @Min(1)
        private int schedulerBatchSize = 20;

        /**
         * Nombre maximum de tentatives TOTALES (niveau 1 + niveau 2 cumules)
         * avant qu'un sync soit declare EN_ERREUR definitif et plus jamais retente.
         *
         * Exemple avec maxAttemptsImmediate=3 et maxTentativesTotal=10 :
         *   - 1ere passe (listener) : tentatives 1, 2, 3 niveau 1, echec -> EN_RETRY (tentatives=3)
         *   - 1ere passe scheduler  : tentatives 4, 5, 6 niveau 1, echec -> EN_RETRY (tentatives=6)
         *   - 2eme passe scheduler  : tentatives 7, 8, 9 niveau 1, echec -> EN_RETRY (tentatives=9)
         *   - 3eme passe scheduler  : tentative 10 niveau 1, echec -> EN_ERREUR (tentatives=10)
         */
        @Min(1)
        private int maxTentativesTotal = 10;
    }
}