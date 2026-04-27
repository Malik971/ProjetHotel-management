package com.example.pastellmock.store;

import com.example.pastellmock.domain.MockDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires PURS du MockDocumentStore.
 *
 * "Purs" signifie : pas de @SpringBootTest, pas d'injection, pas de contexte.
 * On instancie le store en {@code new MockDocumentStore()} et on l'utilise
 * directement. Demarrage en quelques millisecondes au lieu de plusieurs secondes.
 *
 * Couverture :
 *   - Creation : succes nominal, validation des entrees, idD unique
 *   - Lecture : trouvee / non trouvee / null
 *   - Mecanique store : exists, size, clear
 *   - Concurrence : creation simultanee depuis plusieurs threads
 *
 * Conventions :
 *   - JUnit 5 (org.junit.jupiter), AssertJ (assertions fluentes)
 *   - @Nested pour grouper visuellement les scenarios par fonction testee
 *   - @DisplayName pour des libelles lisibles dans les rapports de test
 */
class MockDocumentStoreTest {

    private MockDocumentStore store;

    @BeforeEach
    void setUp() {
        // Une instance neuve pour chaque test : isolation totale
        store = new MockDocumentStore();
    }

    // ============================================================
    // CREATE
    // ============================================================

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("cree un document avec les champs attendus")
        void creates_a_document_with_expected_fields() {
            LocalDateTime before = LocalDateTime.now();

            MockDocument doc = store.create(1L, "reservation-hoteliere");

            // Champs explicitement fournis
            assertThat(doc.getIdEntite()).isEqualTo(1L);
            assertThat(doc.getType()).isEqualTo("reservation-hoteliere");

            // Champs derives par le store
            assertThat(doc.getIdD()).hasSize(12);
            assertThat(doc.getLastAction()).isEqualTo("creation");
            assertThat(doc.getLastActionDate()).isAfterOrEqualTo(before);
            assertThat(doc.getData()).isEmpty();
        }

        @Test
        @DisplayName("le document cree est immediatement retrouvable par son idD")
        void created_document_is_retrievable_by_its_idD() {
            MockDocument created = store.create(1L, "reservation-hoteliere");

            Optional<MockDocument> retrieved = store.findById(created.getIdD());

            assertThat(retrieved).isPresent();
            assertThat(retrieved.get()).isSameAs(created);
        }

        @Test
        @DisplayName("genere un idD unique pour chaque appel")
        void generates_unique_idD_for_each_call() {
            int n = 1000;
            Set<String> ids = new HashSet<>();

            for (int i = 0; i < n; i++) {
                MockDocument doc = store.create(1L, "reservation-hoteliere");
                ids.add(doc.getIdD());
            }

            // Toutes les valeurs sont differentes : aucune collision
            assertThat(ids).hasSize(n);
            assertThat(store.size()).isEqualTo(n);
        }

        @Test
        @DisplayName("rejette idEntite < 1")
        void rejects_idEntite_less_than_one() {
            assertThatThrownBy(() -> store.create(0L, "reservation-hoteliere"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idEntite");

            assertThatThrownBy(() -> store.create(-1L, "reservation-hoteliere"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idEntite");
        }

        @Test
        @DisplayName("rejette type null ou vide")
        void rejects_null_or_blank_type() {
            assertThatThrownBy(() -> store.create(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("type");

            assertThatThrownBy(() -> store.create(1L, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("type");

            assertThatThrownBy(() -> store.create(1L, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("type");
        }
    }

    // ============================================================
    // FIND BY ID
    // ============================================================

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("retourne Optional vide quand le document n'existe pas")
        void returns_empty_optional_when_document_does_not_exist() {
            Optional<MockDocument> result = store.findById("inexistant");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("retourne Optional vide quand idD est null")
        void returns_empty_optional_when_idD_is_null() {
            Optional<MockDocument> result = store.findById(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("retourne le document quand il existe")
        void returns_document_when_it_exists() {
            MockDocument created = store.create(1L, "reservation-hoteliere");

            Optional<MockDocument> result = store.findById(created.getIdD());

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo("reservation-hoteliere");
            assertThat(result.get().getIdEntite()).isEqualTo(1L);
        }
    }

    // ============================================================
    // EXISTS / SIZE / CLEAR
    // ============================================================

    @Nested
    @DisplayName("exists() / size() / clear()")
    class StoreMechanicsTests {

        @Test
        @DisplayName("exists() retourne false pour un idD inconnu ou null")
        void exists_returns_false_for_unknown_or_null_idD() {
            assertThat(store.exists("inexistant")).isFalse();
            assertThat(store.exists(null)).isFalse();
        }

        @Test
        @DisplayName("exists() retourne true apres creation")
        void exists_returns_true_after_creation() {
            MockDocument doc = store.create(1L, "reservation-hoteliere");

            assertThat(store.exists(doc.getIdD())).isTrue();
        }

        @Test
        @DisplayName("size() reflete le nombre de documents stockes")
        void size_reflects_stored_documents_count() {
            assertThat(store.size()).isZero();

            store.create(1L, "type-a");
            store.create(1L, "type-b");
            store.create(2L, "type-a");

            assertThat(store.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("clear() vide le store")
        void clear_empties_the_store() {
            store.create(1L, "type-a");
            store.create(1L, "type-b");
            assertThat(store.size()).isEqualTo(2);

            store.clear();

            assertThat(store.size()).isZero();
        }
    }

    // ============================================================
    // CONCURRENCE
    // ============================================================

    @Nested
    @DisplayName("concurrence")
    class ConcurrencyTests {

        @Test
        @DisplayName("creations paralleles depuis 50 threads ne perdent aucun document")
        void parallel_creations_from_many_threads_lose_no_document() throws Exception {
            int threadCount = 50;
            int creationsPerThread = 100;
            int expectedTotal = threadCount * creationsPerThread;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            AtomicInteger errors = new AtomicInteger(0);

            try {
                for (int t = 0; t < threadCount; t++) {
                    executor.submit(() -> {
                        try {
                            for (int i = 0; i < creationsPerThread; i++) {
                                store.create(1L, "type-stress");
                            }
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        }
                    });
                }
            } finally {
                executor.shutdown();
                boolean completed = executor.awaitTermination(10, TimeUnit.SECONDS);
                assertThat(completed)
                        .as("toutes les taches doivent se terminer en moins de 10s")
                        .isTrue();
            }

            assertThat(errors.get()).as("aucune erreur dans les threads").isZero();
            assertThat(store.size())
                    .as("tous les documents crees sont presents (pas de race condition)")
                    .isEqualTo(expectedTotal);
        }
    }
}