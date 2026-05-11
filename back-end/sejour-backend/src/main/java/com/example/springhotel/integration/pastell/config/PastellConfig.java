package com.example.springhotel.integration.pastell.config;

import com.example.springhotel.integration.pastell.security.PastellCredentialsProvider;
import com.example.springhotel.integration.pastell.security.RotatingBasicAuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

/**
 * Configuration Spring de l'integration Pastell.
 *
 * Cette configuration est CONDITIONNELLE : les beans ne sont crees que si
 * {@code pastell.enabled=true}.
 *
 * Evolution Lot 6 : selection automatique de l'interceptor d'authentification
 * via le bean {@link PastellCredentialsProvider} declare dans
 * {@link PastellCredentialsConfig} (classe separee pour eviter une reference
 * circulaire avec l'injection par champ ci-dessous).
 *
 *   - Si le bean est present (mode rotatif, master-secret defini), on utilise
 *     {@link RotatingBasicAuthInterceptor} qui derive le mot de passe a chaque appel.
 *   - Sinon, on retombe sur le comportement legacy : {@link BasicAuthenticationInterceptor}
 *     avec username/password statiques.
 *
 * Choix d'injection : le {@code credentialsProvider} est injecte par champ avec
 * {@code @Autowired(required = false)}. Cela permet :
 *   - Au runtime Spring : injection automatique si le bean existe, null sinon.
 *   - Dans les tests qui instancient PastellConfig a la main (Lot 3 et suivants) :
 *     le champ reste null, la logique se rabat sur l'auth statique, aucun
 *     changement requis dans les tests existants.
 */
@Configuration
@EnableConfigurationProperties(PastellProperties.class)
@EnableRetry
@EnableScheduling
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellConfig {

    private static final Logger log = LoggerFactory.getLogger(PastellConfig.class);

    public static final String PASTELL_REST_CLIENT = "pastellRestClient";

    private final PastellProperties properties;

    /**
     * Provider de credentials rotatifs. Injecte uniquement si le bean est declare
     * dans {@link PastellCredentialsConfig} (c'est-a-dire en mode rotatif, quand
     * la propriete {@code pastell.master-secret} est definie).
     * Reste null en mode statique et dans les tests qui instancient cette classe
     * a la main sans contexte Spring.
     */
    @Autowired(required = false)
    private PastellCredentialsProvider credentialsProvider;

    public PastellConfig(PastellProperties properties) {
        properties.validateIfEnabled();
        this.properties = properties;
    }

    /**
     * RestClient dedie aux appels Pastell.
     *
     * Configure avec :
     *   - Base URL
     *   - Auth Basic : rotative (Lot 6) ou statique (legacy), selon la presence du bean credentials
     *   - Timeouts de connexion et de lecture
     *   - User-Agent identifiable
     *   - Interceptor de logging discret
     */
    @Bean(PASTELL_REST_CLIENT)
    public RestClient pastellRestClient() {
        log.info("Initialisation du RestClient Pastell (base URL = {}, timeout = {}ms)",
                properties.getUrl(), properties.getTimeoutMs());

        ClientHttpRequestInterceptor authInterceptor;
        if (credentialsProvider != null) {
            authInterceptor = new RotatingBasicAuthInterceptor(credentialsProvider);
            log.info("Auth interceptor : RotatingBasicAuthInterceptor (username derive = {}).",
                    credentialsProvider.getUsername());
        } else {
            log.info("Auth interceptor : BasicAuthenticationInterceptor statique (username = {}).",
                    properties.getUsername());
            authInterceptor = new BasicAuthenticationInterceptor(
                    properties.getUsername(),
                    properties.getPassword());
        }

        return RestClient.builder()
                .baseUrl(properties.getUrl())
                .requestInterceptor(authInterceptor)
                .requestInterceptor(new PastellLoggingInterceptor())
                .defaultHeader(HttpHeaders.USER_AGENT, "Sejour-Backend/1.0 (Pastell-Integration)")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .requestFactory(requestFactory())
                .build();
    }

    @Bean("pastellRetryTemplate")
    public RetryTemplate pastellRetryTemplate() {
        PastellProperties.Retry retryProps = properties.getRetry();
        log.info("Initialisation du RetryTemplate Pastell : maxAttempts={}, initialDelay={}ms, multiplier={}, maxDelay={}ms",
                retryProps.getMaxAttemptsImmediate(),
                retryProps.getInitialDelayMs(),
                retryProps.getMultiplier(),
                retryProps.getMaxDelayMs());

        return RetryTemplate.builder()
                .maxAttempts(retryProps.getMaxAttemptsImmediate())
                .exponentialBackoff(
                        retryProps.getInitialDelayMs(),
                        retryProps.getMultiplier(),
                        retryProps.getMaxDelayMs())
                .retryOn(com.example.springhotel.integration.pastell.client.PastellApiException.class)
                .build();
    }

    private org.springframework.http.client.ClientHttpRequestFactory requestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        return factory;
    }

    /**
     * Interceptor qui logue les appels Pastell de maniere structuree,
     * sans jamais exposer les credentials (header Authorization NON logue).
     */
    private static final class PastellLoggingInterceptor implements ClientHttpRequestInterceptor {

        private static final Logger logger = LoggerFactory.getLogger("com.example.springhotel.integration.pastell.http");

        @Override
        public ClientHttpResponse intercept(HttpRequest request,
                                            byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            long start = System.nanoTime();
            try {
                ClientHttpResponse response = execution.execute(request, body);
                long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

                int statusCode = response.getStatusCode().value();
                String logLine = String.format("Pastell call: %s %s -> %d (%dms)",
                        request.getMethod(),
                        request.getURI().getPath(),
                        statusCode,
                        durationMs);

                if (statusCode >= 500) {
                    logger.error(logLine);
                } else if (statusCode >= 400) {
                    logger.warn(logLine);
                } else {
                    logger.info(logLine);
                }
                return response;
            } catch (IOException e) {
                long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
                logger.error("Pastell call FAILED: {} {} after {}ms - {}",
                        request.getMethod(),
                        request.getURI().getPath(),
                        durationMs,
                        e.getMessage());
                throw e;
            }
        }
    }
}