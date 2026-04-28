package com.example.pastellmock.controller;

import com.example.pastellmock.store.MockDocumentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration MockMvc du DocumentController.
 *
 * Couverture : les 8 scenarios HTTP fondamentaux pour create-document
 * et detail-document.
 *
 * Approche : on ne mocke RIEN, on utilise le vrai store en memoire
 * et la vraie config Spring Security. C'est un test "boite grise" qui
 * verifie le systeme integre, pas une unite isolee.
 *
 * Pourquoi @SpringBootTest et non @WebMvcTest ?
 *   - @WebMvcTest charge uniquement la couche web. Pratique pour des
 *     tests rapides quand on mocke les couches en dessous.
 *   - @SpringBootTest charge le contexte complet : controller, store,
 *     security, exception handler, le tout reel.
 *   - Pour un mock qui sera teste de bout en bout par des clients reels
 *     plus tard, le test integre est plus fidele. Le cout est mineur
 *     (quelques secondes au demarrage du contexte).
 *
 * Pourquoi @TestPropertySource ?
 *   - On override les credentials du mock pour que le test soit
 *     independant des variables d'environnement de la machine.
 *   - Garantit que le test marche en CI sans configuration prealable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "pastell.mock.username=test-user",
        "pastell.mock.password=test-pwd"
})
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Le store reel. On l'injecte pour deux raisons :
     *   1. Le clear() avant chaque test (isolation entre scenarios)
     *   2. La verification directe d'effets de bord ("le document EST bien
     *      dans le store apres POST") dans certains tests.
     */
    @Autowired
    private MockDocumentStore store;

    /**
     * Pour parser le JSON de reponse quand on a besoin de recuperer
     * dynamiquement un id_d genere par le mock.
     */
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * Vidage du store avant chaque test. Pourquoi ?
     *   - Le MockDocumentStore est un @Component Spring : Spring n'en cree
     *     qu'une seule instance partagee dans toute la JVM, y compris entre
     *     les tests d'une meme classe.
     *   - Sans clear(), un test qui cree un document polluerait les tests
     *     suivants ("la base contient deja un document").
     *   - Le clear() garantit que chaque test demarre dans un etat connu
     *     et reproductible.
     */
    @BeforeEach
    void resetStore() {
        store.clear();
    }

    // ============================================================
    // POST create-document
    // ============================================================

    @Nested
    @DisplayName("POST /api/v2/entite/{idEntite}/document")
    class CreateDocumentTests {

        @Test
        @DisplayName("avec credentials + form-data valide -> 201 Created + JSON Pastell")
        void create_with_valid_credentials_and_form_data_returns_201() throws Exception {
            mockMvc.perform(multipart("/api/v2/entite/1/document")
                            .param("type", "reservation-hoteliere")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // L'id_d est genere : on ne connait pas sa valeur exacte
                    // mais on verifie qu'il est present, non vide, et de la bonne longueur
                    .andExpect(jsonPath("$.id_d").isString())
                    .andExpect(jsonPath("$.id_d").isNotEmpty())
                    .andExpect(jsonPath("$.id_d", org.hamcrest.Matchers.hasLength(12)))
                    // Le bloc info est present avec tous ses champs
                    .andExpect(jsonPath("$.info.id_d").exists())
                    .andExpect(jsonPath("$.info.id_e").value(1))
                    .andExpect(jsonPath("$.info.type").value("reservation-hoteliere"))
                    .andExpect(jsonPath("$.info.last_action").value("creation"))
                    // Le format de date suit la convention Pastell : "yyyy-MM-dd HH:mm:ss"
                    // On verifie le pattern, pas la valeur exacte (qui depend de l'instant T)
                    .andExpect(jsonPath("$.info.last_action_date",
                            org.hamcrest.Matchers.matchesPattern(
                                    "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")));

            // Verification d'effet de bord : le document est bien dans le store
            assertThat(store.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("avec Content-Type application/json -> 415 Unsupported Media Type (mode strict)")
        void create_with_json_content_type_returns_415() throws Exception {
            mockMvc.perform(post("/api/v2/entite/1/document")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"reservation-hoteliere\"}")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error_message",
                            org.hamcrest.Matchers.containsString("Content-Type non supporte")));

            // Aucun document ne doit avoir ete cree
            assertThat(store.size()).isZero();
        }

        @Test
        @DisplayName("sans le champ 'type' -> 400 Bad Request")
        void create_without_type_field_returns_400() throws Exception {
            mockMvc.perform(multipart("/api/v2/entite/1/document")
                            .param("autre_champ", "valeur")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error_message",
                            org.hamcrest.Matchers.containsString("type")));

            assertThat(store.size()).isZero();
        }

        @Test
        @DisplayName("sans credentials -> 401 Unauthorized (Spring Security)")
        void create_without_credentials_returns_401() throws Exception {
            mockMvc.perform(multipart("/api/v2/entite/1/document")
                            .param("type", "reservation-hoteliere"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate",
                            org.hamcrest.Matchers.startsWith("Basic")));

            assertThat(store.size()).isZero();
        }

        @Test
        @DisplayName("avec mauvais mot de passe -> 401 Unauthorized")
        void create_with_wrong_password_returns_401() throws Exception {
            mockMvc.perform(multipart("/api/v2/entite/1/document")
                            .param("type", "reservation-hoteliere")
                            .with(httpBasic("test-user", "wrong-password")))
                    .andExpect(status().isUnauthorized());

            assertThat(store.size()).isZero();
        }
    }

    // ============================================================
    // GET detail-document
    // ============================================================

    @Nested
    @DisplayName("GET /api/v2/entite/{idEntite}/document/{idD}")
    class DetailDocumentTests {

        @Test
        @DisplayName("sur un id_d inexistant -> 404 Not Found avec format Pastell")
        void get_unknown_idD_returns_404() throws Exception {
            mockMvc.perform(get("/api/v2/entite/1/document/inexistant")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error_message",
                            org.hamcrest.Matchers.containsString("Document introuvable")));
        }

        @Test
        @DisplayName("sans credentials -> 401 Unauthorized")
        void get_without_credentials_returns_401() throws Exception {
            mockMvc.perform(get("/api/v2/entite/1/document/some-id"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ============================================================
    // SCENARIO INTEGRE
    // ============================================================

    @Nested
    @DisplayName("scenario integre create -> get")
    class IntegratedScenarioTests {

        @Test
        @DisplayName("POST puis GET sur le meme id_d retourne le document cree")
        void post_then_get_returns_the_created_document() throws Exception {
            // 1. POST : creer le document
            MvcResult createResult = mockMvc.perform(multipart("/api/v2/entite/1/document")
                            .param("type", "reservation-hoteliere")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isCreated())
                    .andReturn();

            // Extraire l'id_d retourne par le mock
            String responseJson = createResult.getResponse().getContentAsString();
            JsonNode root = jsonMapper.readTree(responseJson);
            String idD = root.get("id_d").asText();

            assertThat(idD).hasSize(12);

            // 2. GET : recuperer le meme document via son id_d
            mockMvc.perform(get("/api/v2/entite/1/document/" + idD)
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    // Les metadonnees coherentes avec celles du POST
                    .andExpect(jsonPath("$.info.id_d").value(idD))
                    .andExpect(jsonPath("$.info.id_e").value(1))
                    .andExpect(jsonPath("$.info.type").value("reservation-hoteliere"))
                    .andExpect(jsonPath("$.info.last_action").value("creation"))
                    // data vide a la creation (Pastell cree toujours un document vide)
                    .andExpect(jsonPath("$.data").isMap())
                    .andExpect(jsonPath("$.data").isEmpty())
                    // action_possible contient "modification" apres une creation
                    .andExpect(jsonPath("$.action_possible").isArray())
                    .andExpect(jsonPath("$.action_possible[0]").value("modification"));
        }
    }
}