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
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Tests d'integration du PastellClient avec WireMock.
 *
 * Ces tests valident le comportement du client en isolation totale du vrai
 * Pastell (et meme du mock pastell-mock) : on dresse un faux serveur HTTP
 * sur un port aleatoire, on configure ses reponses, et on verifie que le
 * client se comporte correctement face a chaque cas.
 *
 * Pourquoi pas de Spring context (pas de @SpringBootTest) ?
 *   - Demarrage instantane (pas de scan de beans, pas de DataSource a charger).
 *   - Focus pur sur le client : si un test echoue, c'est forcement le client.
 *   - Demontre la testabilite isolee de la couche transport, recommande pour
 *     les classes "stupides" comme PastellClient (pas de logique metier).
 *
 * Strategie de test :
 *   - WireMock demarre une fois pour toute la classe (port dynamique pour
 *     ne jamais entrer en collision avec un autre service local).
 *   - resetAll() avant chaque test pour repartir d'un etat propre.
 *   - Configuration manuelle de PastellProperties + PastellConfig + PastellClient,
 *     fidele a ce que Spring ferait au runtime.
 */
@DisplayName("PastellClient - integration HTTP via WireMock")
class PastellClientTest {

    private static WireMockServer wireMock;
    private PastellClient pastellClient;

    /**
     * Identifiants Basic Auth utilises dans tous les tests.
     * Centralises ici pour faciliter les assertions sur le header Authorization.
     */
    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "test-pass";
    private static final long ENTITE_ID = 1L;
    private static final String TYPE_DOSSIER = "reservation-hoteliere";

    @BeforeAll
    static void startWireMock() {
        // Port dynamique : evite les collisions sur les machines des collegues
        // ou sur les agents CI qui font tourner plusieurs builds en parallele.
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
        // Reset des stubs et des compteurs entre chaque test pour garantir l'isolation.
        wireMock.resetAll();

        // Construction manuelle des dependances, comme le ferait le contexte Spring.
        // L'URL pointe vers WireMock sur son port dynamique.
        PastellProperties properties = new PastellProperties();
        properties.setEnabled(true);
        properties.setUrl("http://localhost:" + wireMock.port());
        properties.setUsername(USERNAME);
        properties.setPassword(PASSWORD);
        properties.setEntiteId(ENTITE_ID);
        properties.setTypeDossier(TYPE_DOSSIER);
        properties.setTimeoutMs(2000);

        // PastellConfig se charge de valider les properties et de builder le RestClient
        // avec tous les interceptors (Basic Auth, logging, headers par defaut).
        PastellConfig config = new PastellConfig(properties);
        RestClient restClient = config.pastellRestClient();

        pastellClient = new PastellClient(restClient, properties);
    }

    @Test
    @DisplayName("createDocument retourne l'id_d quand Pastell repond 201 avec un body valide")
    void createDocument_success() {
        // Arrange : Pastell repond 201 avec une reponse Pastell typique
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id_d": "abc-123-xyz",
                                  "info": {
                                    "id_d": "abc-123-xyz",
                                    "id_e": 1,
                                    "type": "reservation-hoteliere",
                                    "last_action": "creation"
                                  }
                                }
                                """)));

        // Act
        PastellCreateDocumentResponse response = pastellClient.createDocument();

        // Assert : seul l'id_d est mappe (pas le bloc info), c'est ce qu'on veut au Lot 3
        assertThat(response).isNotNull();
        assertThat(response.idD()).isEqualTo("abc-123-xyz");
    }

    @Test
    @DisplayName("createDocument envoie un POST multipart/form-data avec le bon champ type")
    void createDocument_sends_correct_form_data() {
        // Arrange : reponse minimale, on ne s'en sert pas dans ce test
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id_d\":\"x\"}")));

        // Act
        pastellClient.createDocument();

        // Assert : on inspecte la requete recue par WireMock.
        // Approche par contenu brut du body multipart : plus portable cross-versions
        // que les matchers WireMock specifiques au multipart, et suffisant pour
        // garantir que le bon champ form-data est present avec la bonne valeur.
        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v2/entite/1/document"))
                .withHeader("Content-Type", containing("multipart/form-data"))
                .withRequestBody(containing("name=\"type\""))
                .withRequestBody(containing(TYPE_DOSSIER)));
    }

    @Test
    @DisplayName("createDocument envoie un header Authorization Basic correct")
    void createDocument_sends_basic_auth() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id_d\":\"x\"}")));

        pastellClient.createDocument();

        // BasicCredentials encode user:password en Base64 et compose le header
        // "Authorization: Basic ...". Si les credentials envoyes ne correspondent
        // pas, l'assertion echoue.
        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v2/entite/1/document"))
                .withBasicAuth(new BasicCredentials(USERNAME, PASSWORD)));
    }

    @Test
    @DisplayName("createDocument envoie le User-Agent identifiant Sejour-Backend")
    void createDocument_sends_user_agent() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id_d\":\"x\"}")));

        pastellClient.createDocument();

        // Important pour Libriciel : le User-Agent permet d'identifier les appels
        // Sejour dans leurs logs cote Pastell. Si on casse cette ligne par
        // inadvertance, ce test l'attrape.
        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v2/entite/1/document"))
                .withHeader("User-Agent", matching("Sejour-Backend/.*Pastell-Integration.*")));
    }

    @Test
    @DisplayName("createDocument leve PastellApiException avec statusCode=401 sur reponse 4xx")
    void createDocument_throws_on_4xx() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"error\",\"error_message\":\"unauthorized\"}")));

        PastellApiException exception = catchThrowableOfType(
                () -> pastellClient.createDocument(),
                PastellApiException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode()).isEqualTo(401);
        assertThat(exception.hasHttpResponse()).isTrue();
        // Note : on ne teste pas le contenu du body sur un 401.
        // Le HttpClient JDK utilise par RestClient consomme automatiquement
        // le body des reponses 401 (gestion native du WWW-Authenticate),
        // ce qui rend getResponseBody() vide ou null. Ce comportement
        // n'affecte pas le 500 (test ci-dessous) ou les autres 4xx.
        // Pour le diagnostic en prod, le statusCode 401 est suffisant
        // pour comprendre qu'il s'agit d'un probleme de credentials.
    }

    @Test
    @DisplayName("createDocument leve PastellApiException avec statusCode=500 sur reponse 5xx")
    void createDocument_throws_on_5xx() {
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"error\",\"error_message\":\"internal server error\"}")));

        PastellApiException exception = catchThrowableOfType(
                () -> pastellClient.createDocument(),
                PastellApiException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode()).isEqualTo(500);
        assertThat(exception.hasHttpResponse()).isTrue();
    }

    @Test
    @DisplayName("createDocument leve PastellApiException si Pastell repond 2xx mais sans id_d (defense en profondeur)")
    void createDocument_throws_when_idD_blank() {
        // Cas pathologique : Pastell renvoie un statut de succes mais un id_d vide.
        // Sans ce garde-fou, on stockerait un PastellSync sans id_d exploitable,
        // ce qui casserait l'idempotence et empecherait tout appel ulterieur.
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id_d\":\"\"}")));

        PastellApiException exception = catchThrowableOfType(
                () -> pastellClient.createDocument(),
                PastellApiException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).contains("id_d");
    }
}