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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Tests d'integration des actions de demonstration du PastellClient avec WireMock.
 * <p>
 * Couvre les deux methodes ajoutees pour la demo interactive :
 *   - getDocument : lecture de l'etape circuit et des actions encore possibles,
 *   - doAction : transition d'etape (validation, confirmation, terminaison, annulation).
 * </p>
 * Meme strategie que PastellClientTest : faux serveur HTTP sur port dynamique,
 * construction manuelle des dependances, aucun contexte Spring.
 */
@DisplayName("PastellClient - actions de demonstration via WireMock")
class PastellClientActionTest {

    private static WireMockServer wireMock;
    private PastellClient pastellClient;

    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "test-pass";
    private static final long ENTITE_ID = 1L;
    private static final String ID_D = "0e2ebd294169";

    private static final String ACTION_PATH = "/api/v2/entite/1/document/" + ID_D + "/action";
    private static final String DETAIL_PATH = "/api/v2/entite/1/document/" + ID_D;

    private static final String ACTION_RESPONSE_BODY = """
            {
              "result": "ok",
              "info": {
                "id_d": "0e2ebd294169",
                "id_e": 1,
                "type": "reservation-hoteliere",
                "last_action": "validee",
                "last_action_date": "2026-04-28 15:30:00"
              },
              "action_possible": ["confirmation", "annulation"]
            }
            """;

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
        properties.setEntiteId(ENTITE_ID);
        properties.setTypeDossier("reservation-hoteliere");
        properties.setTimeoutMs(2000);

        PastellConfig config = new PastellConfig(properties);
        RestClient restClient = config.pastellRestClient();

        pastellClient = new PastellClient(restClient, properties);
    }

    @Test
    @DisplayName("doAction parse le nouvel etat du dossier (last_action et action_possible)")
    void doAction_parses_new_state() {
        wireMock.stubFor(post(urlPathEqualTo(ACTION_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ACTION_RESPONSE_BODY)));

        PastellDocumentState state = pastellClient.doAction(ID_D, "validation");

        assertThat(state).isNotNull();
        assertThat(state.result()).isEqualTo("ok");
        assertThat(state.info()).isNotNull();
        assertThat(state.info().lastAction()).isEqualTo("validee");
        assertThat(state.actionPossible()).containsExactly("confirmation", "annulation");
    }

    @Test
    @DisplayName("doAction envoie un POST multipart avec le champ action et l'auth Basic")
    void doAction_sends_form_data_and_basic_auth() {
        wireMock.stubFor(post(urlPathEqualTo(ACTION_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ACTION_RESPONSE_BODY)));

        pastellClient.doAction(ID_D, "validation");

        wireMock.verify(postRequestedFor(urlPathEqualTo(ACTION_PATH))
                .withHeader("Content-Type", containing("multipart/form-data"))
                .withRequestBody(containing("name=\"action\""))
                .withRequestBody(containing("validation"))
                .withBasicAuth(new BasicCredentials(USERNAME, PASSWORD)));
    }

    @Test
    @DisplayName("doAction leve PastellApiException quand le connecteur refuse l'action (400)")
    void doAction_throws_on_refused_transition() {
        wireMock.stubFor(post(urlPathEqualTo(ACTION_PATH))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"error\",\"error_message\":\"transition invalide\"}")));

        PastellApiException exception = catchThrowableOfType(
                () -> pastellClient.doAction(ID_D, "terminaison"),
                PastellApiException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode()).isEqualTo(400);
        assertThat(exception.hasHttpResponse()).isTrue();
    }

    @Test
    @DisplayName("getDocument parse le detail du dossier (info et action_possible)")
    void getDocument_parses_detail() {
        String detailBody = """
                {
                  "info": {
                    "id_d": "0e2ebd294169",
                    "id_e": 1,
                    "type": "reservation-hoteliere",
                    "last_action": "creation",
                    "last_action_date": "2026-04-28 15:00:00"
                  },
                  "action_possible": ["validation", "annulation"]
                }
                """;
        wireMock.stubFor(get(urlPathEqualTo(DETAIL_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(detailBody)));

        PastellDocumentState state = pastellClient.getDocument(ID_D);

        assertThat(state).isNotNull();
        assertThat(state.info().idD()).isEqualTo(ID_D);
        assertThat(state.info().lastAction()).isEqualTo("creation");
        assertThat(state.actionPossible()).containsExactly("validation", "annulation");
    }
}
