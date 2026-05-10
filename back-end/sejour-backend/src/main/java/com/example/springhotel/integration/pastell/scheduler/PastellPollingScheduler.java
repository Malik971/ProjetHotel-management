package com.example.springhotel.integration.pastell.scheduler;

import com.example.springhotel.integration.pastell.service.PastellInboundSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler qui declenche le polling descendant Pastell -> Sejour (Lot 5).
 *<p>
 * Toutes les {@code pastell.polling.interval-ms} millisecondes (defaut 30s),
 * appelle {@link PastellInboundSyncService#runPollOnce()} pour aller chercher
 * les nouvelles entrees du journal Pastell et appliquer les changements de
 * statut sur les reservations Sejour.
 *<p>
 * Pourquoi fixedDelay et pas fixedRate ?
 *   - fixedDelay attend la fin de la passe precedente avant de demarrer le delai.
 *     Si une passe prend 5 secondes, la prochaine demarre 30s apres la fin.
 *     Pas de chevauchement, pas de risque de polling concurrent qui se marche dessus.
 *   - fixedRate planifie au temps T+30s meme si la passe precedente n'est pas finie.
 *     Risque de double traitement si on a un coup de bourre cote Pastell.
 *   - Coherent avec le pattern utilise par PastellRetryScheduler (Lot 4).
 *<p>
 * Pourquoi un initialDelay de 10 secondes ?
 *   - Au demarrage, l'application met quelques secondes a etre operationnelle :
 *     pool de connexions JDBC, beans Spring, contextes web. Mieux vaut attendre
 *     un peu avant de declencher des appels HTTP sortants.
 *   - 10 secondes est suffisant pour la plupart des deploiements et reste discret
 *     pour les tests d'integration.
 *<p>
 * Conditional :
 *   - {@code pastell.enabled=true} : sans Pastell, pas de polling.
 *   - {@code pastell.polling.enabled=true} : permet de desactiver finement le
 *     polling tout en gardant l'integration Pastell active. Utile en local
 *     quand on travaille sur la sync montante sans vouloir etre derange par
 *     les logs du polling.
 *<p>
 * Pourquoi pas @Async ?
 *   - Le scheduler tourne deja sur un thread dedie de Spring (TaskScheduler),
 *     inutile d'ajouter @Async qui ne ferait que rajouter un thread.
 *   - fixedDelay garantit qu'une passe ne sera pas relancee avant d'avoir fini.
 */
@Component
@ConditionalOnProperty(
        name = {"pastell.enabled", "pastell.polling.enabled"},
        havingValue = "true",
        matchIfMissing = false)
public class PastellPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PastellPollingScheduler.class);

    private final PastellInboundSyncService inboundSyncService;

    public PastellPollingScheduler(PastellInboundSyncService inboundSyncService) {
        this.inboundSyncService = inboundSyncService;
    }

    /**
     * Tick periodique. La frequence est lue depuis pastell.polling.interval-ms.
     *<p>
     * On enveloppe l'appel dans un try/catch large parce qu'on NE VEUT JAMAIS
     * qu'une exception remontant depuis le service tue le scheduler. Si Spring
     * voyait une exception non rattrapee dans une methode @Scheduled, il pourrait
     * (selon la config) arreter de programmer les tics suivants. Le filet ici
     * garantit que le polling continue meme apres un incident isole.
     */
    @Scheduled(
            fixedDelayString = "${pastell.polling.interval-ms}",
            initialDelay = 10_000L)
    public void scheduledPoll() {
        try {
            inboundSyncService.runPollOnce();
        } catch (Exception e) {
            log.error("Pastell polling : exception non rattrapee dans le tick scheduler - {}",
                    e.getMessage(), e);
        }
    }
}