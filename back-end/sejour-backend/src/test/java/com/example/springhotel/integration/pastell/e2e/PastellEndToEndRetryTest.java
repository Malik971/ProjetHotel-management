package com.example.springhotel.integration.pastell.e2e;

import com.example.springhotel.SpringHotelManagementApplication;
import com.example.springhotel.dto.ReservationRequestDTO;
import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.repository.ReservationRepository;
import com.example.springhotel.service.ReservationService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test end-to-end du Lot 4 : flapping Pastell absorbe par le retry niveau 1.
 *
 * Scenario joue :
 *   - Pastell repond 500 deux fois.
 *   - Au 3eme appel, Pastell repond 201 avec un id_d.
 *   - On verifie que le PastellSync finit en OK avec id_d=DOC-OK,
 *     grace au RetryTemplate (3 tentatives, backoff 10/20/40 ms en test).
 *
 * Pourquoi ce test n'utilise pas le scheduler :
 *   - Le scheduler est verifie unitairement par PastellRetrySchedulerTest.
 *   - Le retry niveau 1 suffit a couvrir ce scenario "flapping court".
 *   - On desactive donc explicitement le scheduler via
 *     pastell.retry.scheduler-enabled=false pour eviter qu'il ne demarre
 *     en parallele et complique l'observation de l'etat final.
 *
 * Pourquoi un Awaitility :
 *   - Le listener AFTER_COMMIT est synchrone, mais l'effet de bord (3 appels HTTP
 *     avec backoff) prend quelques dizaines de millisecondes. Awaitility laisse
 *     une marge tout en echouant tot si quelque chose casse.
 *
 * Pourquoi creerReservation(request, null) :
 *   - La signature reelle de ReservationService est creerReservation(ReservationRequestDTO,
 *     String userEmail). On passe null pour userEmail : la reservation est anonyme,
 *     ce qui correspond au flow d'un client invite. On recupere ensuite l'id via
 *     le ReservationResponseDTO retourne.
 */
@SpringBootTest(classes = SpringHotelManagementApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pastell.enabled=true",
        "pastell.mode=MOCK",
        "pastell.username=sejour",
        "pastell.password=test",
        "pastell.entite-id=1",
        "pastell.type-dossier=reservation-hoteliere",
        "pastell.timeout-ms=2000",
        // Reglages retry tres courts pour que le test reste rapide
        "pastell.retry.max-attempts-immediate=3",
        "pastell.retry.initial-delay-ms=10",
        "pastell.retry.max-delay-ms=50",
        "pastell.retry.scheduler-enabled=false"
})
class PastellEndToEndRetryTest extends PastellEndToEndTestBase {

    @Autowired private ReservationService reservationService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PastellSyncRepository pastellSyncRepository;

    @Test
    void deuxEchecs500PuisSucces_pastellSyncFinitEnOK() {
        // Scenario WireMock stateful : echec, echec, succes.
        // L'etat initial est "Started" (convention WireMock).
        wireMock.stubFor(post(urlPathMatching("/api/v2/entite/.*/document"))
                .inScenario("flap")
                .whenScenarioStateIs("Started")
                .willSetStateTo("E1")
                .willReturn(aResponse().withStatus(500).withBody("first")));
        wireMock.stubFor(post(urlPathMatching("/api/v2/entite/.*/document"))
                .inScenario("flap")
                .whenScenarioStateIs("E1")
                .willSetStateTo("E2")
                .willReturn(aResponse().withStatus(500).withBody("second")));
        wireMock.stubFor(post(urlPathMatching("/api/v2/entite/.*/document"))
                .inScenario("flap")
                .whenScenarioStateIs("E2")
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id_d\":\"DOC-OK\"}")));

        ReservationRequestDTO request = buildReservationRequest();
        // Signature reelle : creerReservation(request, userEmail). On passe null
        // pour simuler un client invite (flow d'usage classique).
        ReservationResponseDTO response = reservationService.creerReservation(request, null);

        // Awaitility : on attend jusqu'a 5 secondes que le sync passe en OK.
        // En pratique ca prend ~70 ms (2 retries niveau 1 a 10ms + 20ms de backoff
        // plus le temps reseau local), mais on prend une marge confortable.
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Optional<PastellSync> sync = pastellSyncRepository.findByReservationId(response.getId());
            assertThat(sync).isPresent();
            assertThat(sync.get().getSyncStatus()).isEqualTo(SyncStatus.OK);
            assertThat(sync.get().getPastellDocumentId()).isEqualTo("DOC-OK");
            // tentatives = 1 : du point de vue PastellSyncService, c'est UN appel
            // (avec retries internes au niveau 1 qui ne sont pas comptabilises ici).
            // Si on voulait compter les retries niveau 1 dans tentatives, il faudrait
            // remonter le compteur depuis le RetryTemplate, ce qu'on ne fait pas
            // volontairement (separation des responsabilites).
            assertThat(sync.get().getTentatives()).isEqualTo(1);
        });

        // Cleanup : on supprime la reservation, la cascade SQL ON DELETE CASCADE
        // sur pastell_sync s'occupe de nettoyer le sync associe.
        reservationRepository.deleteById(response.getId());
    }
}