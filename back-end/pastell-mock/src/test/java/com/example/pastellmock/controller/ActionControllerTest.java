package com.example.pastellmock.controller;

import com.example.pastellmock.domain.MockDocument;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration MockMvc du ActionController.
 *
 * Couvre les scenarios principaux du endpoint
 * POST /api/v2/entite/{idEntite}/document/{idD}/action :
 *
 *   - Transition valide : 200 + reponse "ok" + nouvelles actions possibles
 *   - Document inconnu : 404
 *   - Transition invalide (etat / action incompatibles) : 400
 *   - Champ "action" manquant : 400
 *   - JSON refuse (mode strict) : 415
 *   - Sans credentials : 401
 *   - Workflow complet enchaine : preuve d'integration de bout en bout
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "pastell.mock.username=test-user",
        "pastell.mock.password=test-pwd"
})
class ActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockDocumentStore store;

    @BeforeEach
    void resetStore() {
        store.clear();
    }

    /**
     * Helper : cree un document directement via le store (pas via HTTP)
     * pour preparer les scenarios de change-action.
     * Plus rapide qu'un POST initial pour chaque test.
     */
    private MockDocument givenAnExistingDocument() {
        return store.create(1L, "reservation-hoteliere");
    }

    // ============================================================
    // SUCCES
    // ============================================================

    @Nested
    @DisplayName("transitions valides")
    class ValidTransitionTests {

        @Test
        @DisplayName("validation depuis 'creation' -> 200 + 'en-attente-validation'")
        void validation_from_creation_returns_200_with_new_state() throws Exception {
            MockDocument doc = givenAnExistingDocument();

            mockMvc.perform(multipart("/api/v2/entite/1/document/" + doc.getIdD() + "/action")
                            .param("action", "validation")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.result").value("ok"))
                    .andExpect(jsonPath("$.info.id_d").value(doc.getIdD()))
                    .andExpect(jsonPath("$.info.last_action").value("en-attente-validation"))
                    // Depuis "en-attente-validation", actions possibles : annulation, validation
                    .andExpect(jsonPath("$.action_possible",
                            org.hamcrest.Matchers.containsInAnyOrder("annulation", "validation")));
        }

        @Test
        @DisplayName("annulation depuis 'creation' -> 200 + 'annulee', plus aucune action possible")
        void annulation_from_creation_reaches_terminal_state() throws Exception {
            MockDocument doc = givenAnExistingDocument();

            mockMvc.perform(multipart("/api/v2/entite/1/document/" + doc.getIdD() + "/action")
                            .param("action", "annulation")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("ok"))
                    .andExpect(jsonPath("$.info.last_action").value("annulee"))
                    .andExpect(jsonPath("$.action_possible").isArray())
                    .andExpect(jsonPath("$.action_possible").isEmpty());
        }
    }

    // ============================================================
    // ERREURS
    // ============================================================

    @Nested
    @DisplayName("erreurs")
    class ErrorTests {

        @Test
        @DisplayName("document inconnu -> 404")
        void unknown_document_returns_404() throws Exception {
            mockMvc.perform(multipart("/api/v2/entite/1/document/inexistant/action")
                            .param("action", "validation")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error_message",
                            org.hamcrest.Matchers.containsString("Document introuvable")));
        }

        @Test
        @DisplayName("transition invalide (creation + confirmation) -> 400 avec message clair")
        void invalid_transition_returns_400() throws Exception {
            MockDocument doc = givenAnExistingDocument();

            mockMvc.perform(multipart("/api/v2/entite/1/document/" + doc.getIdD() + "/action")
                            .param("action", "confirmation")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error_message",
                            org.hamcrest.Matchers.containsString("confirmation")))
                    .andExpect(jsonPath("$.error_message",
                            org.hamcrest.Matchers.containsString("creation")));

            // Le document n'a PAS ete mute
            MockDocument afterFail = store.findById(doc.getIdD()).orElseThrow();
            org.assertj.core.api.Assertions.assertThat(afterFail.getLastAction())
                    .isEqualTo("creation");
        }

        @Test
        @DisplayName("champ 'action' manquant -> 400")
        void missing_action_field_returns_400() throws Exception {
            MockDocument doc = givenAnExistingDocument();

            mockMvc.perform(multipart("/api/v2/entite/1/document/" + doc.getIdD() + "/action")
                            .param("autre_champ", "valeur")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error_message",
                            org.hamcrest.Matchers.containsString("action")));
        }

        @Test
        @DisplayName("Content-Type JSON -> 415")
        void json_content_type_returns_415() throws Exception {
            MockDocument doc = givenAnExistingDocument();

            mockMvc.perform(post("/api/v2/entite/1/document/" + doc.getIdD() + "/action")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"action\":\"validation\"}")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("sans credentials -> 401")
        void without_credentials_returns_401() throws Exception {
            MockDocument doc = givenAnExistingDocument();

            mockMvc.perform(multipart("/api/v2/entite/1/document/" + doc.getIdD() + "/action")
                            .param("action", "validation"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ============================================================
    // SCENARIO INTEGRE
    // ============================================================

    @Nested
    @DisplayName("scenario integre")
    class IntegratedTests {

        @Test
        @DisplayName("workflow complet : creation -> en-attente -> validee -> confirmee -> terminee")
        void complete_workflow_through_HTTP() throws Exception {
            MockDocument doc = givenAnExistingDocument();
            String url = "/api/v2/entite/1/document/" + doc.getIdD() + "/action";

            // 1. validation -> en-attente-validation
            mockMvc.perform(multipart(url)
                            .param("action", "validation")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.last_action").value("en-attente-validation"));

            // 2. validation -> validee
            mockMvc.perform(multipart(url)
                            .param("action", "validation")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.last_action").value("validee"));

            // 3. confirmation -> confirmee
            mockMvc.perform(multipart(url)
                            .param("action", "confirmation")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.last_action").value("confirmee"));

            // 4. terminaison -> terminee, plus aucune action possible
            mockMvc.perform(multipart(url)
                            .param("action", "terminaison")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.last_action").value("terminee"))
                    .andExpect(jsonPath("$.action_possible").isEmpty());

            // 5. tentative supplementaire -> 400 (etat terminal)
            mockMvc.perform(multipart(url)
                            .param("action", "validation")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isBadRequest());
        }
    }
}