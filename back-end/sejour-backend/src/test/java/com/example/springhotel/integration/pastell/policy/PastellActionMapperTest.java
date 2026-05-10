package com.example.springhotel.integration.pastell.policy;

import com.example.springhotel.entity.Reservation.StatutReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires purs (pas de Spring) pour {@link PastellActionMapper}.
 *<p>
 * Ces tests couvrent la table de decision exhaustivement. Comme le mapper
 * est une fonction pure (pas d'etat, pas de dependances), on peut le tester
 * sans contexte Spring, ce qui rend les tests instantanes et clairs.
 *<p>
 * Strategie :
 *   - On cree UNE instance du mapper en attribut de classe (immutable).
 *   - On structure les tests par cas de figure (action -> statut, conflits).
 *   - On utilise les tests parameterises pour les listes d'actions ou de statuts
 *     qui partagent une meme attente, pour eviter la duplication.
 */
@DisplayName("PastellActionMapper - table de decision action -> StatutReservation")
class PastellActionMapperTest {

    private final PastellActionMapper mapper = new PastellActionMapper();

    // ============================================================
    // resolveTargetStatus
    // ============================================================

    @Nested
    @DisplayName("resolveTargetStatus")
    class ResolveTargetStatus {

        @Test
        @DisplayName("'terminee' -> StatutReservation.TERMINEE")
        void terminee_maps_to_TERMINEE() {
            Optional<StatutReservation> result = mapper.resolveTargetStatus("terminee");
            assertThat(result).contains(StatutReservation.TERMINEE);
        }

        @Test
        @DisplayName("'annulee' -> StatutReservation.ANNULEE")
        void annulee_maps_to_ANNULEE() {
            Optional<StatutReservation> result = mapper.resolveTargetStatus("annulee");
            assertThat(result).contains(StatutReservation.ANNULEE);
        }

        @ParameterizedTest(name = "''{0}'' -> Optional.empty (action neutre)")
        @ValueSource(strings = {"creation", "en-attente-validation", "validee", "confirmee"})
        @DisplayName("Actions neutres (creation, en-attente-validation, validee, confirmee) -> empty")
        void actions_neutres_renvoient_empty(String action) {
            Optional<StatutReservation> result = mapper.resolveTargetStatus(action);
            assertThat(result).isEmpty();
        }

        @ParameterizedTest(name = "''{0}'' -> Optional.empty (action inconnue)")
        @ValueSource(strings = {"modifie", "transmise", "scelle", "n-importe-quoi"})
        @DisplayName("Actions inconnues retournent empty (extension future Pastell)")
        void actions_inconnues_renvoient_empty(String action) {
            Optional<StatutReservation> result = mapper.resolveTargetStatus(action);
            assertThat(result).isEmpty();
        }

        @ParameterizedTest(name = "casse normalisee : ''{0}''")
        @ValueSource(strings = {"TERMINEE", "Terminee", "TermineE", " terminee ", " TERMINEE "})
        @DisplayName("La normalisation de casse + trim fonctionne sur 'terminee'")
        void casse_et_trim_normalises_pour_terminee(String input) {
            assertThat(mapper.resolveTargetStatus(input)).contains(StatutReservation.TERMINEE);
        }

        @ParameterizedTest(name = "casse normalisee : ''{0}''")
        @ValueSource(strings = {"ANNULEE", "Annulee", "AnnULee"})
        @DisplayName("La normalisation de casse fonctionne sur 'annulee'")
        void casse_normalisee_pour_annulee(String input) {
            assertThat(mapper.resolveTargetStatus(input)).contains(StatutReservation.ANNULEE);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        @DisplayName("null, vide ou blanc -> Optional.empty (defensif)")
        void null_vide_blanc_renvoient_empty(String input) {
            assertThat(mapper.resolveTargetStatus(input)).isEmpty();
        }
    }

    // ============================================================
    // isConflict
    // ============================================================

    @Nested
    @DisplayName("isConflict")
    class IsConflict {

        @Test
        @DisplayName("'annulee' Pastell + Sejour TERMINEE = conflit")
        void annulee_sur_terminee_est_un_conflit() {
            assertThat(mapper.isConflict("annulee", StatutReservation.TERMINEE)).isTrue();
        }

        @Test
        @DisplayName("'terminee' Pastell + Sejour ANNULEE = conflit")
        void terminee_sur_annulee_est_un_conflit() {
            assertThat(mapper.isConflict("terminee", StatutReservation.ANNULEE)).isTrue();
        }

        @ParameterizedTest(name = "action ''{0}'' + statut {1} : pas de conflit")
        @CsvSource({
                "annulee,            CONFIRMEE",
                "annulee,            EN_ATTENTE",
                "annulee,            ANNULEE",
                "terminee,           CONFIRMEE",
                "terminee,           EN_ATTENTE",
                "terminee,           TERMINEE",
                "creation,           CONFIRMEE",
                "validee,            CONFIRMEE",
                "confirmee,          CONFIRMEE",
                "en-attente-validation, EN_ATTENTE"
        })
        @DisplayName("Cas non conflictuels : pas de divergence")
        void cas_non_conflictuels_ne_sont_pas_des_conflits(String action, String statut) {
            StatutReservation s = StatutReservation.valueOf(statut.trim());
            assertThat(mapper.isConflict(action, s)).isFalse();
        }

        @Test
        @DisplayName("La detection de conflit est insensible a la casse")
        void casse_normalisee_pour_isConflict() {
            assertThat(mapper.isConflict("ANNULEE", StatutReservation.TERMINEE)).isTrue();
            assertThat(mapper.isConflict(" Terminee ", StatutReservation.ANNULEE)).isTrue();
        }

        @Test
        @DisplayName("null en entree -> pas de conflit (defensif)")
        void null_inputs_pas_de_conflit() {
            assertThat(mapper.isConflict(null, StatutReservation.TERMINEE)).isFalse();
            assertThat(mapper.isConflict("annulee", null)).isFalse();
            assertThat(mapper.isConflict(null, null)).isFalse();
        }
    }
}