package com.example.springhotel.integration.pastell.client;

import com.example.springhotel.integration.pastell.config.PastellConfig;
import com.example.springhotel.integration.pastell.config.PastellProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Tests d'integration de {@link PastellClient#fetchJournalSince(long)} avec WireMock.
 *<p>
 * On separe ces tests de {@link PastellClientTest} parce qu'ils couvrent une
 * methode tres differente (GET avec query param, deserialisation d'un tableau
 * JSON), et que les regrouper aurait fait un fichier de plus de 400 lignes
 * difficile a naviguer.
 *<p>
 * Strategie identique a PastellClientTest : pas de Spring context, port dynamique,
 * resetAll() entre chaque test.
 */
@DisplayName("PastellClient.fetchJournalSince - integration HTTP via WireMock")
class PastellClientJournalTest {

    private static WireMockServer wireMock;
    private PastellClient pastellClient;

    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "test-pass";

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();

        PastellProperties properties = new PastellProperties();
        properties.setEnabled(true);
        properties.setUrl("http://localhost:" + wireMock.port());
        properties.setUsername(USERNAME);
        properties.setPassword(PASSWORD);
        properties.setEntiteId(1L);
        properties.setTypeDossier("reservation-hoteliere");
        properties.setTimeoutMs(2000);

        PastellConfig config = new PastellConfig(properties);
        RestClient restClient = config.pastellRestClient();

        pastellClient = new PastellClient(restClient, properties);
    }

    @Test
    @DisplayName("fetchJournalSince retourne la liste deserialisee quand Pastell repond 200 avec entrees")
    void fetchJournalSince_success_with_entries() {
        // Arrange : le mock renvoie deux entrees au format Pastell
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/journal"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "id_j": 42,
                                    "id_d": "abc-123",
                                    "id_e": 1,
                                    "action": "validee",
                                    "date": "2026-04-28 15:30:00"
                                  },
                                  {
                                    "id_j": 43,
                                    "id_d": "xyz-789",
                                    "id_e": 1,
                                    "action": "annulee",
                                    "date": "2026-04-28 15:32:11"
                                  }
                                ]
                                """)));

        // Act
        List<PastellJournalEntry> entries = pastellClient.fetchJournalSince(40);

        // Assert : taille, ordre et contenu
        assertThat(entries).hasSize(2);

        PastellJournalEntry first = entries.get(0);
        assertThat(first.idJ()).isEqualTo(42);
        assertThat(first.idD()).isEqualTo("abc-123");
        assertThat(first.idEntite()).isEqualTo(1);
        assertThat(first.action()).isEqualTo("validee");
        assertThat(first.date()).isNotNull();
        assertThat(first.date().getYear()).isEqualTo(2026);
        assertThat(first.date().getMonthValue()).isEqualTo(4);
        assertThat(first.date().getDayOfMonth()).isEqualTo(28);

        PastellJournalEntry second = entries.get(1);
        assertThat(second.idJ()).isEqualTo(43);
        assertThat(second.idD()).isEqualTo("xyz-789");
        assertThat(second.action()).isEqualTo("annulee");
    }

    @Test
    @DisplayName("fetchJournalSince retourne une liste vide quand Pastell renvoie []")
    void fetchJournalSince_empty_response() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/journal"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        List<PastellJournalEntry> entries = pastellClient.fetchJournalSince(0);

        assertThat(entries).isEmpty();
    }

    @Test
    @DisplayName("fetchJournalSince transmet bien le parametre since_id_j dans la query string")
    void fetchJournalSince_sends_query_param() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/journal"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        pastellClient.fetchJournalSince(1234L);

        // Assert : WireMock a bien recu le param since_id_j=1234
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/v2/journal"))
                .withQueryParam("since_id_j", equalTo("1234")));
    }

    @Test
    @DisplayName("fetchJournalSince envoie un header Authorization Basic correct")
    void fetchJournalSince_sends_basic_auth() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/journal"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        pastellClient.fetchJournalSince(0);

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/v2/journal"))
                .withBasicAuth(new BasicCredentials(USERNAME, PASSWORD)));
    }

    @Test
    @DisplayName("fetchJournalSince leve PastellApiException sur reponse 5xx")
    void fetchJournalSince_throws_on_5xx() {
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/journal"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"error\",\"error_message\":\"service unavailable\"}")));

        PastellApiException exception = catchThrowableOfType(
                () -> pastellClient.fetchJournalSince(0),
                PastellApiException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode()).isEqualTo(503);
        assertThat(exception.hasHttpResponse()).isTrue();
        assertThat(exception.getMessage()).contains("/api/v2/journal");
    }

    @Test
    @DisplayName("fetchJournalSince ignore les champs inconnus dans la reponse JSON (forward compat)")
    void fetchJournalSince_ignores_unknown_fields() {
        // Pastell pourrait ajouter des champs dans le futur. Le @JsonIgnoreProperties
        // sur PastellJournalEntry doit garantir qu'on ne casse pas la deserialisation.
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/journal"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "id_j": 100,
                                    "id_d": "futur-doc",
                                    "id_e": 1,
                                    "action": "validee",
                                    "date": "2026-05-01 10:00:00",
                                    "champ_futur_pastell": "valeur exotique",
                                    "metadata": {"trace_id": "xyz"}
                                  }
                                ]
                                """)));

        List<PastellJournalEntry> entries = pastellClient.fetchJournalSince(0);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).idD()).isEqualTo("futur-doc");
    }
}