package com.example.springhotel.integration.pastell.client;

import com.example.springhotel.integration.pastell.config.PastellConfig;
import com.example.springhotel.integration.pastell.config.PastellProperties;
import com.example.springhotel.integration.pastell.policy.PastellRetryPolicy;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests d'integration WireMock pour le wrapper de retry niveau 1.
 *
 * Pourquoi WireMock standalone et pas un mock Mockito ?
 *   - On veut verifier que le RetryTemplate fait BIEN des appels HTTP repetes,
 *     pas juste qu'il rappelle une methode Java.
 *   - WireMock permet de simuler un Pastell flapping (echec puis succes) avec
 *     ses scenarios stateful.
 *   - C'est plus pres de la realite de prod : on teste le combo RestClient +
 *     RetryTemplate + politique, pas chaque brique separement.
 */
class PastellClientWithRetryTest {

    private static final String SCENARIO = "pastell-flapping";

    private WireMockServer wireMock;
    private PastellClientWithRetry clientWithRetry;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        // Reglages serres pour que le test ne dure pas une eternite
        PastellProperties props = new PastellProperties();
        props.setEnabled(true);
        props.setUrl("http://localhost:" + wireMock.port());
        props.setUsername("sejour");
        props.setPassword("test");
        props.setEntiteId(1L);
        props.setTypeDossier("reservation-hoteliere");
        props.setTimeoutMs(2000);

        PastellProperties.Retry retry = props.getRetry();
        retry.setMaxAttemptsImmediate(3);
        retry.setInitialDelayMs(10);   // raccourci pour les tests
        retry.setMaxDelayMs(50);
        retry.setMultiplier(2.0);

        // Construction manuelle des dependances : pas de Spring ici, tests rapides.
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutMs());
        factory.setReadTimeout(props.getTimeoutMs());

        RestClient restClient = RestClient.builder()
                .baseUrl(props.getUrl())
                .requestInterceptor(new BasicAuthenticationInterceptor(props.getUsername(), props.getPassword()))
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .requestFactory(factory)
                .build();

        PastellClient client = new PastellClient(restClient, props);

        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(retry.getMaxAttemptsImmediate())
                .exponentialBackoff(retry.getInitialDelayMs(), retry.getMultiplier(), retry.getMaxDelayMs())
                .retryOn(PastellApiException.class)
                .build();

        clientWithRetry = new PastellClientWithRetry(client, retryTemplate, new PastellRetryPolicy());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void succesPremierCoup_unSeulAppel() {
        wireMock.stubFor(post(urlPathMatching("/api/v2/entite/.*/document"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id_d":"DOC-OK","entite":1,"type":"reservation-hoteliere","date_creation":"2026-01-01"}
                                """)));

        PastellCreateDocumentResponse resp = clientWithRetry.createDocumentWithRetry();

        assertThat(resp.idD()).isEqualTo("DOC-OK");
        wireMock.verify(1, postRequestedFor(urlPathMatching("/api/v2/entite/.*/document")));
    }

    @Test
    void deuxEchecs500PuisSucces_troisAppels() {
        // Scenario WireMock : Pastell repond 500 deux fois, puis 201.
        wireMock.stubFor(post(urlPathMatching("/api/v2/entite/.*/document"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs("Started")
                .willSetStateTo("ECHEC_1")
                .willReturn(aResponse().withStatus(500).withBody("oops")));

        wireMock.stubFor(post(urlPathMatching("/api/v2/entite/.*/document"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs("ECHEC_1")
                .willSetStateTo("ECHEC_2")
                .willReturn(aResponse().withStatus(500).withBody("oops")));

        wireMock.stubFor(post(urlPathMatching("/api/v2/entite/.*/document"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs("ECHEC_2")
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id_d":"DOC-RECOVER","entite":1,"type":"reservation-hoteliere","date_creation":"2026-01-01"}
                                """)));

        PastellCreateDocumentResponse resp = clientWithRetry.createDocumentWithRetry();

        assertThat(resp.idD()).isEqualTo("DOC-RECOVER");
        wireMock.verify(3, postRequestedFor(urlPathMatching("/api/v2/entite/.*/document")));
    }

    @Test
    void troisEchecs500_propageException() {
        wireMock.stubFor(post(urlPathMatching("/api/v2/entite/.*/document"))
                .willReturn(aResponse().withStatus(500).withBody("toujours casse")));

        assertThatThrownBy(() -> clientWithRetry.createDocumentWithRetry())
                .isInstanceOf(PastellApiException.class)
                .satisfies(e -> {
                    PastellApiException ex = (PastellApiException) e;
                    assertThat(ex.getStatusCode()).isEqualTo(500);
                });

        wireMock.verify(3, postRequestedFor(urlPathMatching("/api/v2/entite/.*/document")));
    }

    @Test
    void echec401_pasDeRetry_unSeulAppel() {
        // 401 = non-retryable : on doit s'arreter immediatement, sans retry.
        wireMock.stubFor(post(urlPathMatching("/api/v2/entite/.*/document"))
                .willReturn(aResponse().withStatus(401).withBody("auth")));

        assertThatThrownBy(() -> clientWithRetry.createDocumentWithRetry())
                .isInstanceOf(PastellApiException.class)
                .satisfies(e -> assertThat(((PastellApiException) e).getStatusCode()).isEqualTo(401));

        wireMock.verify(1, postRequestedFor(urlPathMatching("/api/v2/entite/.*/document")));
    }
}