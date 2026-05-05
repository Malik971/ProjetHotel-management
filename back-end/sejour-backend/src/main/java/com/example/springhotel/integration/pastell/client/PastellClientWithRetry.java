package com.example.springhotel.integration.pastell.client;

import com.example.springhotel.integration.pastell.policy.PastellRetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Wrapper de retry court (niveau 1) au-dessus de {@link PastellClient}.
 *<p>
 * Role :
 * <p>
 *   - Encapsuler chaque appel Pastell dans un RetryTemplate avec backoff exponentiel.
 *   - Consulter {@link PastellRetryPolicy} pour stopper immediatement le retry
 *     sur une exception non-retryable (ex. 401 mauvais credentials).
 *   - Logger les tentatives pour le diagnostic.
 *<p>
 * Pourquoi un wrapper plutot que d'annoter PastellClient avec @Retryable ?
 * <p>
 *   - PastellClient reste une couche transport pure, testable sans Spring Retry.
 *   - On peut tester la politique de retry isolement (PastellClientWithRetryTest)
 *     sans interference avec la couche transport.
 *   - Le wrapper est explicitement nomme : on voit dans les imports qui appelle
 *     "client direct" et qui appelle "client avec retry". Pas de magie AOP cachee.
 *<p>
 * Pourquoi {@code context.setExhaustedOnly()} et pas une autre exception ?
 * <p>
 *   - Spring Retry permet de signaler au RetryContext "j'ai decide qu'on arretait
 *     la, ne re-essaie pas, propage l'exception telle quelle".
 *   - Solution alternative : creer une RetryableException et NonRetryableException
 *     distinctes et configurer retryOn(RetryableException.class). Mais ca obligerait
 *     a modifier PastellClient pour qu'il choisisse, ce qu'on veut eviter (couplage).
 *<p>
 * Analogie pedagogique :
 * <p>
 *   PastellClient est le standardiste qui compose le numero. PastellClientWithRetry
 *   est le standardiste senior qui dit "ah, ca sonne occupe, on rappelle dans 200ms,
 *   puis 400ms, puis 800ms, et au bout de 3 essais on abandonne pour aujourd'hui".
 *   Si le standardiste rapporte "ce numero n'existe pas" (404), le senior n'insiste
 *   meme pas, il sait que ca ne sert a rien.
 */
@Component
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellClientWithRetry {

    private static final Logger log = LoggerFactory.getLogger(PastellClientWithRetry.class);

    private final PastellClient delegate;
    private final RetryTemplate retryTemplate;
    private final PastellRetryPolicy retryPolicy;

    public PastellClientWithRetry(
            PastellClient delegate,
            @Qualifier("pastellRetryTemplate") RetryTemplate retryTemplate,
            PastellRetryPolicy retryPolicy) {
        this.delegate = delegate;
        this.retryTemplate = retryTemplate;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Cree un dossier Pastell avec retry court automatique en cas d'echec retryable.
     *<p>
     * Comportement :
     * <p>
     *   - Succes au premier coup : retourne la reponse, aucune trace de retry.
     *   - Echec retryable (5xx, NETWORK, 408, 429) : RetryTemplate ressaie selon
     *     la config, jusqu'a maxAttempts ou jusqu'au premier succes.
     *   - Echec non-retryable (4xx hors 408/429, 2xx avec body cassee) : on appelle
     *     {@code context.setExhaustedOnly()}, RetryTemplate propage l'exception
     *     immediatement sans re-essayer.
     *   - Echec final apres N tentatives : la derniere exception est propagee
     *     telle quelle au service appelant.
     *
     * @return la reponse Pastell en cas de succes
     * @throws PastellApiException si toutes les tentatives ont echoue OU si le
     *                              premier appel a leve une erreur non-retryable
     */
    public PastellCreateDocumentResponse createDocumentWithRetry() {
        return retryTemplate.execute(context -> {
            int attemptNumber = context.getRetryCount() + 1;
            try {
                if (attemptNumber > 1) {
                    log.info("Pastell : tentative {} de createDocument (apres echec retryable)", attemptNumber);
                }
                return delegate.createDocument();

            } catch (PastellApiException e) {
                if (!retryPolicy.isRetryable(e)) {
                    // On informe RetryTemplate qu'il ne doit PAS re-essayer.
                    // L'exception est propagee telle quelle a l'appelant.
                    log.warn("Pastell : echec non-retryable a la tentative {} ({}) - abandon immediat",
                            attemptNumber, e.getMessage());
                    context.setExhaustedOnly();
                    throw e;
                }
                // Erreur retryable : on log et on relance, RetryTemplate gere le backoff.
                log.warn("Pastell : echec retryable a la tentative {} ({}) - delegation au RetryTemplate",
                        attemptNumber, e.getMessage());
                throw e;
            }
        });
    }
}