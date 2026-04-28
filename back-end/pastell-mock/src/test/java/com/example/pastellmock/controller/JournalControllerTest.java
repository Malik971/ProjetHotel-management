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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'integration MockMvc du JournalController.
 *
 * Couvre les scenarios principaux de GET /api/v2/journal :
 *
 *   - Journal vide : 200 + tableau vide
 *   - Journal avec entrees : retournees dans l'ordre d'idJ croissant
 *   - Filtrage par since_id_j : seules les entrees plus recentes
 *   - Sans credentials : 401
 *   - Format JSON : tableau (pas d'enveloppe)
 *
 * Chaque entree du journal a la forme :
 *   {"id_j": N, "id_d": "...", "id_e": N, "action": "...", "date": "..."}
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "pastell.mock.username=test-user",
        "pastell.mock.password=test-pwd"
})
class JournalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockDocumentStore store;

    @BeforeEach
    void resetStore() {
        store.clear();
    }

    // ============================================================
    // CAS NOMINAUX
    // ============================================================

    @Nested
    @DisplayName("lecture du journal")
    class ReadTests {

        @Test
        @DisplayName("journal vide -> 200 + tableau JSON vide []")
        void empty_journal_returns_empty_array() throws Exception {
            mockMvc.perform(get("/api/v2/journal")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("apres N creations -> N entrees retournees, idJ croissant")
        void after_N_creations_returns_N_entries() throws Exception {
            store.create(1L, "type-a");
            store.create(1L, "type-b");
            store.create(2L, "type-c");

            mockMvc.perform(get("/api/v2/journal")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)))
                    // idJ croissants
                    .andExpect(jsonPath("$[0].id_j").value(1))
                    .andExpect(jsonPath("$[1].id_j").value(2))
                    .andExpect(jsonPath("$[2].id_j").value(3))
                    // Tous "creation"
                    .andExpect(jsonPath("$[0].action").value("creation"))
                    .andExpect(jsonPath("$[1].action").value("creation"))
                    .andExpect(jsonPath("$[2].action").value("creation"));
        }

        @Test
        @DisplayName("entrees ont tous les champs Pastell attendus")
        void entries_have_all_pastell_fields() throws Exception {
            MockDocument doc = store.create(7L, "reservation-hoteliere");

            mockMvc.perform(get("/api/v2/journal")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id_j").value(1))
                    .andExpect(jsonPath("$[0].id_d").value(doc.getIdD()))
                    .andExpect(jsonPath("$[0].id_e").value(7))
                    .andExpect(jsonPath("$[0].action").value("creation"))
                    .andExpect(jsonPath("$[0].date",
                            org.hamcrest.Matchers.matchesPattern(
                                    "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")));
        }

        @Test
        @DisplayName("changeAction ajoute une nouvelle entree avec l'etat cible")
        void changeAction_adds_entry_with_target_state() throws Exception {
            MockDocument doc = store.create(1L, "reservation-hoteliere");
            store.changeAction(doc.getIdD(), "validation");

            mockMvc.perform(get("/api/v2/journal")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                    .andExpect(jsonPath("$[0].action").value("creation"))
                    .andExpect(jsonPath("$[1].action").value("en-attente-validation"));
        }
    }

    // ============================================================
    // FILTRAGE
    // ============================================================

    @Nested
    @DisplayName("filtrage par since_id_j")
    class FilterTests {

        @Test
        @DisplayName("since_id_j=2 ne retourne que les entrees > 2")
        void since_id_j_filters_strictly_greater() throws Exception {
            store.create(1L, "type-a"); // idJ=1
            store.create(1L, "type-b"); // idJ=2
            store.create(1L, "type-c"); // idJ=3
            store.create(1L, "type-d"); // idJ=4

            mockMvc.perform(get("/api/v2/journal")
                            .param("since_id_j", "2")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                    .andExpect(jsonPath("$[0].id_j").value(3))
                    .andExpect(jsonPath("$[1].id_j").value(4));
        }

        @Test
        @DisplayName("since_id_j au-dela du max -> tableau vide")
        void since_id_j_beyond_max_returns_empty() throws Exception {
            store.create(1L, "type-a");
            store.create(1L, "type-b");

            mockMvc.perform(get("/api/v2/journal")
                            .param("since_id_j", "999")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("since_id_j=0 (defaut) retourne tout")
        void since_id_j_zero_returns_all() throws Exception {
            store.create(1L, "type-a");
            store.create(1L, "type-b");

            mockMvc.perform(get("/api/v2/journal")
                            .param("since_id_j", "0")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
        }
    }

    // ============================================================
    // SECURITE
    // ============================================================

    @Nested
    @DisplayName("securite")
    class SecurityTests {

        @Test
        @DisplayName("sans credentials -> 401")
        void without_credentials_returns_401() throws Exception {
            mockMvc.perform(get("/api/v2/journal"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ============================================================
    // SCENARIO POLLING (le cas d'usage du Lot 5)
    // ============================================================

    @Nested
    @DisplayName("scenario polling")
    class PollingScenarioTests {

        @Test
        @DisplayName("polling itere : on retient le dernier idJ vu et on demande la suite")
        void polling_with_lastSeenIdJ_returns_only_new_entries() throws Exception {
            // Phase 1 : 2 documents existent au demarrage du client polling
            store.create(1L, "type-a");
            store.create(1L, "type-b");

            // Le client demande tout (premier appel)
            mockMvc.perform(get("/api/v2/journal")
                            .param("since_id_j", "0")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
            // Le client retient : dernier idJ vu = 2

            // Phase 2 : le mock evolue cote backoffice (2 nouveaux events)
            store.create(1L, "type-c");
            MockDocument doc = store.create(1L, "type-d");
            store.changeAction(doc.getIdD(), "validation");

            // Le client repolls a partir de son dernier idJ vu (2)
            mockMvc.perform(get("/api/v2/journal")
                            .param("since_id_j", "2")
                            .with(httpBasic("test-user", "test-pwd")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)))
                    .andExpect(jsonPath("$[0].id_j").value(3))
                    .andExpect(jsonPath("$[1].id_j").value(4))
                    .andExpect(jsonPath("$[2].id_j").value(5))
                    .andExpect(jsonPath("$[2].action").value("en-attente-validation"));
        }
    }
}