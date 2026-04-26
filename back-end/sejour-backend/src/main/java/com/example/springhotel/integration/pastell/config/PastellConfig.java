package com.example.springhotel.integration.pastell.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

/**
 * Configuration Spring de l'integration Pastell.
 *
 * Cette configuration est CONDITIONNELLE : les beans ne sont crees que si
 * {@code pastell.enabled=true}. Quand l'integration est desactivee (defaut),
 * aucun bean Pastell n'est present dans le contexte, ce qui empeche toute
 * injection accidentelle et rend la fonctionnalite totalement invisible.
 *
 * DevRel note : ce pattern "ConditionalOnProperty" est recommande pour toute
 * integration optionnelle. Il permet a un partenaire de builder l'application
 * sans avoir encore de Pastell, et d'activer la fonctionnalite quand il est pret.
 */
@Configuration
@EnableConfigurationProperties(PastellProperties.class)
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellConfig {

    private static final Logger log = LoggerFactory.getLogger(PastellConfig.class);

    /**
     * Nom du bean RestClient dedie a Pastell.
     * Utilise ce nom explicite (plutot que le defaut) permet d'avoir plusieurs
     * RestClient dans le contexte sans collision (ex. si on ajoute un client HTTP
     * vers un autre service tiers plus tard).
     */
    public static final String PASTELL_REST_CLIENT = "pastellRestClient";

    private final PastellProperties properties;

    public PastellConfig(PastellProperties properties) {
        // Validation conditionnelle au demarrage :
        // si enabled=true, tous les champs obligatoires doivent etre renseignes
        properties.validateIfEnabled();
        this.properties = properties;
    }

    /**
     * RestClient dedie aux appels Pastell.
     *
     * Configure avec :
     *   - Base URL : prefixe toutes les requetes par l'URL de la plateforme Pastell
     *   - HTTP Basic Auth : Pastell n'accepte que ce mode (pas de JWT, pas d'OAuth2)
     *   - Timeouts de connexion et de lecture : pour eviter qu'un Pastell lent
     *     ne bloque le pool de threads du listener asynchrone (Lot 4)
     *   - User-Agent identifiable : permet a Libriciel d'identifier les appels
     *     Sejour dans leurs logs, utile pour le support en cas d'incident
     *   - Interceptor de logging discret : trace chaque appel sans jamais
     *     logger les credentials (important pour la securite)
     *
     * @return un RestClient configure, pret a etre injecte dans PastellClient (Lot 3)
     */
    @Bean(PASTELL_REST_CLIENT)
    public RestClient pastellRestClient() {
        log.info("Initialisation du RestClient Pastell (base URL = {}, timeout = {}ms)",
                properties.getUrl(), properties.getTimeoutMs());

        return RestClient.builder()
                .baseUrl(properties.getUrl())
                .requestInterceptor(new BasicAuthenticationInterceptor(
                        properties.getUsername(),
                        properties.getPassword()))
                .requestInterceptor(new PastellLoggingInterceptor())
                .defaultHeader(HttpHeaders.USER_AGENT, "Sejour-Backend/1.0 (Pastell-Integration)")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .requestFactory(requestFactory())
                .build();
    }

    /**
     * Fabrique de requetes HTTP avec timeouts explicites.
     * Utilise le JDK HttpClient sous-jacent (Java 11+, disponible en Java 25).
     */
    private org.springframework.http.client.ClientHttpRequestFactory requestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        return factory;
    }

    /**
     * Interceptor qui logue les appels Pastell de maniere structuree,
     * sans jamais exposer les credentials (header Authorization NON logue).
     *
     * Format : "Pastell call: METHOD /api/xxx.php -> 200 OK (142ms)"
     *
     * Niveau INFO en succes, WARN en erreur 4xx, ERROR en 5xx ou exception.
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