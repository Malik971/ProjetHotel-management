package com.example.springhotel.integration.pastell.e2e;

import com.example.springhotel.dto.ReservationRequestDTO;
import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.entity.Reservation.StatutReservation;
import com.example.springhotel.integration.pastell.entity.PastellPollingCursor;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.repository.PastellPollingCursorRepository;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.integration.pastell.service.PastellInboundSyncService;
import com.example.springhotel.repository.ReservationRepository;
import com.example.springhotel.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test end-to-end du polling descendant Pastell -> Sejour (Lot 5).
 *<p>
 * Scenario du test :
 *   1. On cree une reservation (statut CONFIRMEE) qui declenche la sync montante.
 *   2. Le mock Pastell renvoie l'id_d "doc-e2e-99" : le sync est OK.
 *   3. On stub GET /api/v2/journal pour simuler "un agent a annule le dossier
 *      doc-e2e-99 cote Pastell".
 *   4. On declenche un poll manuellement (runPollOnce()).
 *   5. On verifie que :
 *      - La reservation est bascule en ANNULEE.
 *      - Le PastellSync a son etat connu = "annulee".
 *      - Le curseur est avance a idJ=42.
 *<p>
 * Pourquoi pastell.polling.enabled=false ?
 *   - On veut un test deterministe : on declenche le poll quand on veut, pas
 *     quand le scheduler decide. Sinon le timer pourrait tirer pendant le setup
 *     et fausser les assertions.
 *   - On verifie que le service est bien invocable en isolation, ce qui est
 *     plus utile que tester le timer (couvert par un test scheduler dedie).
 *<p>
 * Pourquoi pastell.retry.scheduler-enabled=false ?
 *   - Pour eviter que le scheduler de retry tire en plein milieu du test et
 *     fasse des appels Pastell parasites.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pastell.enabled=true",
        "pastell.mode=MOCK",
        "pastell.username=test-user",
        "pastell.password=test-password",
        "pastell.entite-id=1",
        "pastell.type-dossier=reservation-hoteliere",
        "pastell.timeout-ms=2000",
        // On desactive les schedulers pour avoir un controle total sur le timing
        "pastell.polling.enabled=false",
        "pastell.retry.scheduler-enabled=false"
        // pastell.url est injecte dynamiquement par PastellEndToEndTestBase
})
@DisplayName("Pastell E2E - polling descendant : agent annule dans Pastell -> Sejour bascule")
class PastellPollingEndToEndTest extends PastellEndToEndTestBase {

    @Autowired private ReservationService reservationService;
    @Autowired private PastellSyncRepository pastellSyncRepository;
    @Autowired private PastellPollingCursorRepository cursorRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PastellInboundSyncService inboundSyncService;

    @Test
    @DisplayName("Agent annule un dossier dans Pastell, Sejour bascule la reservation en ANNULEE apres un poll")
    void agent_annule_dans_pastell_sejour_bascule_apres_poll() {
        // ============================================================
        // PHASE 1 : sync montante - creer la reservation et son PastellSync
        // ============================================================

        // Stub : Pastell repond OK a la creation avec id_d "doc-e2e-99"
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id_d": "doc-e2e-99",
                                  "info": { "id_d": "doc-e2e-99", "id_e": 1, "type": "reservation-hoteliere" }
                                }
                                """)));

        ReservationRequestDTO request = buildReservationRequest();
        ReservationResponseDTO response = reservationService.creerReservation(request, null);

        // Verification : la reservation est creee en EN_ATTENTE (depuis le lot signature,
        // la confirmation exige un visa admin - la panne Pastell ne bloque pas la creation)
        // et le sync montant est OK.
        assertThat(response.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        Optional<PastellSync> syncAvantPoll = pastellSyncRepository.findByReservationId(response.getId());
        assertThat(syncAvantPoll).isPresent();
        assertThat(syncAvantPoll.get().getSyncStatus()).isEqualTo(SyncStatus.OK);
        assertThat(syncAvantPoll.get().getPastellDocumentId()).isEqualTo("doc-e2e-99");
        // L'etat connu cote Pastell est encore null (jamais polled)
        assertThat(syncAvantPoll.get().getPastellEtatDernierConnu()).isNull();

        // ============================================================
        // PHASE 2 : sync descendante - simuler une annulation cote Pastell
        // ============================================================

        // Reset des stubs WireMock pour ne plus repondre a /document
        // (le scheduler de retry est de toute facon desactive, mais on est defensif)
        wireMock.resetAll();

        // Stub : le journal Pastell contient une nouvelle entree d'annulation
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/journal"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {
                                    "id_j": 42,
                                    "id_d": "doc-e2e-99",
                                    "id_e": 1,
                                    "action": "annulee",
                                    "date": "2026-04-28 16:00:00"
                                  }
                                ]
                                """)));

        // Declenche le polling manuellement (un seul tick)
        int traites = inboundSyncService.runPollOnce();

        // ============================================================
        // PHASE 3 : verifications post-polling
        // ============================================================

        // 1 entree traitee
        assertThat(traites).isEqualTo(1);

        // Le client a bien appele GET /api/v2/journal avec since_id_j=0
        // (curseur initial a 0 puisque c'est le premier poll)
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/v2/journal"))
                .withQueryParam("since_id_j", equalTo("0")));

        // La reservation a bascule en ANNULEE
        Reservation reservationApres = reservationRepository.findById(response.getId()).orElseThrow();
        assertThat(reservationApres.getStatut()).isEqualTo(StatutReservation.ANNULEE);

        // Le sync a son etat connu mis a jour (toujours OK, pas DIVERGENCE)
        PastellSync syncApres = pastellSyncRepository.findByReservationId(response.getId()).orElseThrow();
        assertThat(syncApres.getPastellEtatDernierConnu()).isEqualTo("annulee");
        assertThat(syncApres.getDerniereSynchro()).isNotNull();
        assertThat(syncApres.getSyncStatus()).isEqualTo(SyncStatus.OK);

        // Le curseur est avance a 42
        Optional<PastellPollingCursor> curseur = cursorRepository.findCursor();
        assertThat(curseur).isPresent();
        assertThat(curseur.get().getLastProcessedIdJ()).isEqualTo(42L);
        assertThat(curseur.get().getLastPolledAt()).isNotNull();
    }

    @Test
    @DisplayName("Deuxieme tick avec le meme journal : pas de double traitement (idempotence par curseur)")
    void deuxieme_tick_idempotent_grace_au_curseur() {
        // Setup : un sync montant
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id_d\":\"doc-idempotent\",\"info\":{}}")));

        ReservationResponseDTO response = reservationService.creerReservation(buildReservationRequest(), null);

        // Premier tick : journal contient l'annulation
        wireMock.resetAll();
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/journal"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"id_j": 100, "id_d": "doc-idempotent", "id_e": 1,
                                  "action": "annulee", "date": "2026-05-01 10:00:00"}]
                                """)));

        int traitesPasse1 = inboundSyncService.runPollOnce();
        assertThat(traitesPasse1).isEqualTo(1);

        // Deuxieme tick : Pastell renvoie le MEME journal, mais le curseur
        // est maintenant a 100, donc le mock devrait recevoir since_id_j=100.
        // Pour simplifier, on stub /journal pour qu'il renvoie une liste vide
        // si on demande since_id_j=100 (comportement attendu d'un vrai Pastell).
        wireMock.resetAll();
        wireMock.stubFor(get(urlPathEqualTo("/api/v2/journal"))
                .withQueryParam("since_id_j", equalTo("100"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        int traitesPasse2 = inboundSyncService.runPollOnce();

        // 0 entree traitee : le curseur a fait son travail, on ne re-traite pas idJ=100
        assertThat(traitesPasse2).isZero();

        // La reservation est restee ANNULEE (pas de double bascule)
        Reservation res = reservationRepository.findById(response.getId()).orElseThrow();
        assertThat(res.getStatut()).isEqualTo(StatutReservation.ANNULEE);

        // Le client a bien demande since_id_j=100 au deuxieme tick
        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/v2/journal"))
                .withQueryParam("since_id_j", equalTo("100")));
    }
}