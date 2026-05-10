package com.example.springhotel.integration.pastell.scheduler;

import com.example.springhotel.integration.pastell.config.PastellProperties;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.policy.PastellRetryPolicy;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.integration.pastell.service.PastellSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Scheduler de reprise des synchronisations Pastell echouees (Lot 4 niveau 2).
 *<p>
 * Role :
 *   - Toutes les {@code pastell.retry.scheduler-interval-ms} millisecondes,
 *     ramasse les PastellSync en EN_RETRY (ou PENDING orphelins) et les retente.
 *   - Limite chaque passe a {@code pastell.retry.scheduler-batch-size} pour
 *     ne pas noyer Pastell apres une longue panne.
 *   - Trie en FIFO (du plus ancien au plus recent) pour l'equite.
 *   - Avant d'appeler le service, filtre les syncs dont l'erreur n'est plus
 *     retryable (ex. si la politique a change entre temps) ou qui ont depasse
 *     {@code maxTentativesTotal} : ces syncs sont basculees en EN_ERREUR ici meme.
 *<p>
 * Pourquoi un fixedDelay et pas un fixedRate ou un cron ?
 *   - fixedDelay attend la fin de la passe precedente AVANT de demarrer le delai.
 *     Si une passe prend 30 secondes, la prochaine demarre 5 minutes apres la fin.
 *     Pas de chevauchement, pas de risque d'instances qui se marchent dessus.
 *   - fixedRate planifie au temps T+X meme si la passe precedente n'est pas finie :
 *     risque de chevauchement et de double-traitement si on a eu un coup de bourre.
 *   - cron est plus expressif mais overkill pour un job a frequence reguliere.
 *<p>
 * Pourquoi pas {@code @Async} sur le scheduler ?
 *   - Le scheduler tourne deja sur un thread dedie de Spring (TaskScheduler).
 *     Inutile d'ajouter @Async, ce ne ferait que rajouter un thread inutile.
 *   - Si une passe est lente, fixedDelay garantit qu'elle ne sera pas relancee
 *     avant d'avoir fini.
 *<p>
 * Pourquoi pas une transaction qui englobe toute la passe ?
 *   - On veut que chaque sync soit retraite dans sa propre transaction,
 *     isolee des autres. Si le sync N echoue de maniere catastrophique
 *     (NPE, OOM partiel, etc.), les syncs N+1, N+2... doivent quand meme etre
 *     traites dans la meme passe. C'est PastellSyncService.retraiterSync
 *     qui ouvre sa propre transaction REQUIRES_NEW.
 *<p>
 * Conditional :
 *   - {@code pastell.enabled=true} sinon aucune intention de parler a Pastell.
 *   - {@code pastell.retry.scheduler-enabled=true} pour pouvoir desactiver
 *     finement le scheduler sans toucher au reste (utile en local).
 */
@Component
@ConditionalOnProperty(
        name = {"pastell.enabled", "pastell.retry.scheduler-enabled"},
        havingValue = "true")
public class PastellRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PastellRetryScheduler.class);

    /** Statuts qui sont candidats au retraitement. */
    private static final Set<SyncStatus> STATUTS_A_RETRAITER =
            Set.of(SyncStatus.EN_RETRY, SyncStatus.PENDING);

    private final PastellSyncRepository pastellSyncRepository;
    private final PastellSyncService pastellSyncService;
    private final PastellRetryPolicy retryPolicy;
    private final PastellProperties properties;

    public PastellRetryScheduler(
            PastellSyncRepository pastellSyncRepository,
            PastellSyncService pastellSyncService,
            PastellRetryPolicy retryPolicy,
            PastellProperties properties) {
        this.pastellSyncRepository = pastellSyncRepository;
        this.pastellSyncService = pastellSyncService;
        this.retryPolicy = retryPolicy;
        this.properties = properties;
    }

    /**
     * Methode planifiee : declenchee automatiquement par Spring.
     *
     * Le {@code initialDelay} (10s) evite que le scheduler tourne pendant le boot
     * de l'application, quand le contexte n'est pas forcement pret pour des
     * appels HTTP sortants.
     *
     * Cette methode delegue a {@link #runRetryBatch()} qui contient toute la
     * logique. La separation permet de tester runRetryBatch directement,
     * sans dependre du timer JUnit.
     */
    @Scheduled(
            fixedDelayString = "${pastell.retry.scheduler-interval-ms}",
            initialDelay = 10_000L)
    public void scheduledRetry() {
        runRetryBatch();
    }

    /**
     * Logique du batch, exposee publiquement pour les tests.
     *
     * Etapes :
     *   1. Recuperer jusqu'a {@code batchSize} syncs candidats, FIFO.
     *   2. Pour chaque sync :
     *      - si depasse maxTentativesTotal : bascule directe en EN_ERREUR
     *      - si derniere erreur non-retryable : bascule directe en EN_ERREUR
     *      - sinon : delegue a PastellSyncService.retraiterSync()
     *
     * @return le nombre de syncs effectivement traites (utile en test)
     */
    public int runRetryBatch() {
        int batchSize = properties.getRetry().getSchedulerBatchSize();
        int maxTotal = properties.getRetry().getMaxTentativesTotal();

        List<PastellSync> candidats = pastellSyncRepository
                .findCandidatsRetraitement(STATUTS_A_RETRAITER, PageRequest.of(0, batchSize));

        if (candidats.isEmpty()) {
            log.debug("Pastell : passe scheduler - aucun sync a retraiter");
            return 0;
        }

        log.info("Pastell : passe scheduler - {} sync(s) a retraiter (batch max = {})",
                candidats.size(), batchSize);

        int traites = 0;
        for (PastellSync sync : candidats) {
            try {
                if (doitEtreBasculeEnErreur(sync, maxTotal)) {
                    forcerEnErreur(sync.getId(), motifBascule(sync, maxTotal));
                    continue;
                }
                pastellSyncService.retraiterSync(sync.getId());
                traites++;
            } catch (Exception e) {
                // Filet de securite : retraiterSync n'est pas cense propager,
                // mais si une erreur exotique arrive on protege la passe.
                log.error("Pastell : exception inattendue lors du retraitement du sync {} - {}",
                        sync.getId(), e.getMessage(), e);
            }
        }

        log.info("Pastell : passe scheduler terminee - {} sync(s) retraite(s)", traites);
        return traites;
    }

    /**
     * Un sync doit etre bascule en EN_ERREUR sans retry si :
     *   - son nombre de tentatives a deja atteint maxTotal,
     *     ou
     *   - sa derniere erreur stockee n'est pas retryable.
     *
     * Le PENDING orphelin (jamais appele) n'a pas de derniereErreur, donc
     * isRetryable("") retourne true (on retry par defaut). Il sera retraite.
     */
    private boolean doitEtreBasculeEnErreur(PastellSync sync, int maxTotal) {
        if (sync.getTentatives() >= maxTotal) {
            return true;
        }
        if (sync.getSyncStatus() == SyncStatus.EN_RETRY
                && !retryPolicy.isRetryable(sync.getDerniereErreur())) {
            return true;
        }
        return false;
    }

    private String motifBascule(PastellSync sync, int maxTotal) {
        if (sync.getTentatives() >= maxTotal) {
            return "Quota de tentatives epuise (" + sync.getTentatives() + "/" + maxTotal + ")";
        }
        return "Erreur non-retryable detectee a la passe scheduler : " + sync.getDerniereErreur();
    }

    /**
     * Bascule un sync en EN_ERREUR, dans une transaction propre.
     *
     * Sans @Transactional REQUIRES_NEW, l'update se ferait dans la transaction
     * implicite du scheduler thread, ce qui ne pose pas de probleme particulier,
     * mais on prefere etre coherent avec retraiterSync : chaque sync = sa transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void forcerEnErreur(Long syncId, String motif) {
        pastellSyncRepository.findById(syncId).ifPresent(sync -> {
            sync.setSyncStatus(SyncStatus.EN_ERREUR);
            sync.setDerniereSynchro(LocalDateTime.now());
            // On NE concatene PAS le motif a derniereErreur pour ne pas
            // perdre la cause technique d'origine. Le motif va dans les logs.
            pastellSyncRepository.save(sync);
            log.error("Pastell : sync {} (reservation {}) bascule en EN_ERREUR par le scheduler - {}",
                    syncId, sync.getReservationId(), motif);
        });
    }
}