package com.example.pastellmock.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests purs de DocumentTransitions.
 *
 * "Purs" : pas de Spring, pas de store, pas de mock. La classe sous test
 * est un objet metier sans dependance externe, on la teste directement.
 *
 * Couverture :
 *   - Transitions valides (toutes les fleches du diagramme)
 *   - Transitions invalides (etat inconnu, action inconnue, etat terminal)
 *   - Symetrie : canTransition() coherent avec resolveTargetState()
 *   - Robustesse : null en entree
 *   - availableActions() : ordre stable, contenu correct, etats terminaux
 */
class DocumentTransitionsTest {

    private DocumentTransitions transitions;

    @BeforeEach
    void setUp() {
        transitions = new DocumentTransitions();
    }

    // ============================================================
    // RESOLVE TARGET STATE - cas valides
    // ============================================================

    @Nested
    @DisplayName("resolveTargetState() - transitions valides")
    class ValidTransitions {

        @Test
        @DisplayName("creation + validation -> en-attente-validation")
        void creation_validation_to_en_attente() {
            assertThat(transitions.resolveTargetState("creation", "validation"))
                    .contains("en-attente-validation");
        }

        @Test
        @DisplayName("creation + annulation -> annulee")
        void creation_annulation_to_annulee() {
            assertThat(transitions.resolveTargetState("creation", "annulation"))
                    .contains("annulee");
        }

        @Test
        @DisplayName("en-attente-validation + validation -> validee")
        void en_attente_validation_to_validee() {
            assertThat(transitions.resolveTargetState("en-attente-validation", "validation"))
                    .contains("validee");
        }

        @Test
        @DisplayName("en-attente-validation + annulation -> annulee")
        void en_attente_annulation_to_annulee() {
            assertThat(transitions.resolveTargetState("en-attente-validation", "annulation"))
                    .contains("annulee");
        }

        @Test
        @DisplayName("validee + confirmation -> confirmee")
        void validee_confirmation_to_confirmee() {
            assertThat(transitions.resolveTargetState("validee", "confirmation"))
                    .contains("confirmee");
        }

        @Test
        @DisplayName("validee + annulation -> annulee")
        void validee_annulation_to_annulee() {
            assertThat(transitions.resolveTargetState("validee", "annulation"))
                    .contains("annulee");
        }

        @Test
        @DisplayName("confirmee + terminaison -> terminee")
        void confirmee_terminaison_to_terminee() {
            assertThat(transitions.resolveTargetState("confirmee", "terminaison"))
                    .contains("terminee");
        }
    }

    // ============================================================
    // RESOLVE TARGET STATE - cas invalides
    // ============================================================

    @Nested
    @DisplayName("resolveTargetState() - transitions invalides")
    class InvalidTransitions {

        @Test
        @DisplayName("etat inconnu -> empty")
        void unknown_state_returns_empty() {
            assertThat(transitions.resolveTargetState("etat-inexistant", "validation"))
                    .isEmpty();
        }

        @Test
        @DisplayName("action inconnue -> empty")
        void unknown_action_returns_empty() {
            assertThat(transitions.resolveTargetState("creation", "action-bizarre"))
                    .isEmpty();
        }

        @Test
        @DisplayName("etat terminal 'terminee' n'accepte aucune action")
        void terminee_is_terminal() {
            assertThat(transitions.resolveTargetState("terminee", "validation")).isEmpty();
            assertThat(transitions.resolveTargetState("terminee", "confirmation")).isEmpty();
            assertThat(transitions.resolveTargetState("terminee", "annulation")).isEmpty();
            assertThat(transitions.resolveTargetState("terminee", "terminaison")).isEmpty();
        }

        @Test
        @DisplayName("etat terminal 'annulee' n'accepte aucune action")
        void annulee_is_terminal() {
            assertThat(transitions.resolveTargetState("annulee", "validation")).isEmpty();
            assertThat(transitions.resolveTargetState("annulee", "confirmation")).isEmpty();
            assertThat(transitions.resolveTargetState("annulee", "terminaison")).isEmpty();
        }

        @Test
        @DisplayName("creation ne peut pas sauter directement a confirmation")
        void no_skip_from_creation_to_confirmation() {
            assertThat(transitions.resolveTargetState("creation", "confirmation"))
                    .isEmpty();
        }

        @Test
        @DisplayName("null en entree -> empty")
        void null_inputs_return_empty() {
            assertThat(transitions.resolveTargetState(null, "validation")).isEmpty();
            assertThat(transitions.resolveTargetState("creation", null)).isEmpty();
            assertThat(transitions.resolveTargetState(null, null)).isEmpty();
        }
    }

    // ============================================================
    // CAN TRANSITION
    // ============================================================

    @Nested
    @DisplayName("canTransition() - coherence avec resolveTargetState()")
    class CanTransitionTests {

        @Test
        @DisplayName("retourne true pour les transitions valides")
        void returns_true_for_valid_transitions() {
            assertThat(transitions.canTransition("creation", "validation")).isTrue();
            assertThat(transitions.canTransition("validee", "confirmation")).isTrue();
        }

        @Test
        @DisplayName("retourne false pour les transitions invalides")
        void returns_false_for_invalid_transitions() {
            assertThat(transitions.canTransition("creation", "confirmation")).isFalse();
            assertThat(transitions.canTransition("terminee", "validation")).isFalse();
            assertThat(transitions.canTransition(null, "validation")).isFalse();
            assertThat(transitions.canTransition("creation", null)).isFalse();
        }
    }

    // ============================================================
    // AVAILABLE ACTIONS
    // ============================================================

    @Nested
    @DisplayName("availableActions()")
    class AvailableActionsTests {

        @Test
        @DisplayName("creation propose validation et annulation")
        void creation_proposes_validation_and_annulation() {
            Set<String> actions = transitions.availableActions("creation");
            assertThat(actions).containsExactlyInAnyOrder("validation", "annulation");
        }

        @Test
        @DisplayName("validee propose confirmation et annulation")
        void validee_proposes_confirmation_and_annulation() {
            Set<String> actions = transitions.availableActions("validee");
            assertThat(actions).containsExactlyInAnyOrder("confirmation", "annulation");
        }

        @Test
        @DisplayName("confirmee ne propose que terminaison")
        void confirmee_proposes_only_terminaison() {
            Set<String> actions = transitions.availableActions("confirmee");
            assertThat(actions).containsExactly("terminaison");
        }

        @Test
        @DisplayName("etats terminaux retournent un set vide")
        void terminal_states_return_empty_set() {
            assertThat(transitions.availableActions("terminee")).isEmpty();
            assertThat(transitions.availableActions("annulee")).isEmpty();
        }

        @Test
        @DisplayName("etat inconnu retourne un set vide")
        void unknown_state_returns_empty_set() {
            assertThat(transitions.availableActions("etat-inexistant")).isEmpty();
        }

        @Test
        @DisplayName("null retourne un set vide")
        void null_returns_empty_set() {
            assertThat(transitions.availableActions(null)).isEmpty();
        }

        @Test
        @DisplayName("le set retourne est immuable")
        void returned_set_is_unmodifiable() {
            Set<String> actions = transitions.availableActions("creation");
            assertThat(actions).isNotEmpty();
            // Tentative de modification -> doit lever UnsupportedOperationException
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> actions.add("hack")
            );
        }
    }

    // ============================================================
    // SCENARIO COMPLET
    // ============================================================

    @Nested
    @DisplayName("scenario nominal complet")
    class FullScenarioTest {

        @Test
        @DisplayName("creation -> en-attente-validation -> validee -> confirmee -> terminee")
        void complete_happy_path() {
            String state = "creation";

            Optional<String> next1 = transitions.resolveTargetState(state, "validation");
            assertThat(next1).contains("en-attente-validation");
            state = next1.get();

            Optional<String> next2 = transitions.resolveTargetState(state, "validation");
            assertThat(next2).contains("validee");
            state = next2.get();

            Optional<String> next3 = transitions.resolveTargetState(state, "confirmation");
            assertThat(next3).contains("confirmee");
            state = next3.get();

            Optional<String> next4 = transitions.resolveTargetState(state, "terminaison");
            assertThat(next4).contains("terminee");
            state = next4.get();

            // Etat terminal : plus aucune transition
            assertThat(transitions.availableActions(state)).isEmpty();
        }
    }
}