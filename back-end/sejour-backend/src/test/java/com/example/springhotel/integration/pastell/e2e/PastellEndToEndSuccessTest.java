package com.example.springhotel.integration.pastell.e2e;

import com.example.springhotel.dto.ReservationRequestDTO;
import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test end-to-end du flow heureux : reservation creee, Pastell repond OK,
 * un PastellSync en OK est persiste avec l'id_d recu.
 *
 * Si TOUT cela passe vert, on a la garantie que l'integration Pastell fonctionne
 * de bout en bout en mode "happy path".
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
        "pastell.timeout-ms=2000"
        // pastell.url est injecte dynamiquement par PastellEndToEndTestBase
})
@DisplayName("Pastell E2E - flow heureux")
class PastellEndToEndSuccessTest extends PastellEndToEndTestBase {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PastellSyncRepository pastellSyncRepository;

    @Test
    @DisplayName("creerReservation declenche un appel Pastell et persiste un PastellSync OK")
    void creerReservation_creates_reservation_and_pastellSync_OK() {
        // Arrange : Pastell mock repond OK avec un id_d
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id_d": "e2e-doc-123",
                                  "info": { "id_d": "e2e-doc-123", "id_e": 1, "type": "reservation-hoteliere" }
                                }
                                """)));

        // Act : on cree une reservation via le service
        ReservationRequestDTO request = buildReservationRequest();
        ReservationResponseDTO response = reservationService.creerReservation(request, null);

        // Assert 1 : la reservation a ete creee avec succes
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getCodeConfirmation()).isNotBlank();

        // Assert 2 : Pastell a bien recu l'appel attendu.
        // En @TransactionalEventListener(AFTER_COMMIT) SANS @Async, le listener
        // s'execute synchroniquement juste apres le commit dans le meme thread.
        // Au moment ou creerReservation() retourne, l'appel Pastell est deja fait.
        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v2/entite/1/document"))
                .withHeader("Content-Type", containing("multipart/form-data"))
                .withRequestBody(containing("reservation-hoteliere")));

        // Assert 3 : un PastellSync a ete persiste en OK avec l'id_d recu
        Optional<PastellSync> syncOpt = pastellSyncRepository.findByReservationId(response.getId());
        assertThat(syncOpt).isPresent();
        PastellSync sync = syncOpt.get();
        assertThat(sync.getSyncStatus()).isEqualTo(SyncStatus.OK);
        assertThat(sync.getPastellDocumentId()).isEqualTo("e2e-doc-123");
        assertThat(sync.getTentatives()).isEqualTo(1);
        assertThat(sync.getDerniereSynchro()).isNotNull();
        assertThat(sync.getDerniereErreur()).isNull();
    }
}