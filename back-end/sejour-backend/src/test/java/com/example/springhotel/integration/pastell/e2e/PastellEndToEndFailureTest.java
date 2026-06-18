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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test end-to-end du scenario "Pastell down" : meme si Pastell echoue,
 * la reservation reste creee et confirmee.
 *
 * C'est LE test qui valide le principe "Spring autorite, Pastell satellite".
 * Si ce test echoue, c'est qu'un client peut etre bloque dans sa reservation
 * a cause d'une panne Pastell, ce qui est inacceptable.
 *
 * Note : l'idempotence (ne pas creer 2 dossiers Pastell pour la meme reservation)
 * est testee exhaustivement par les tests unitaires Mockito de PastellSyncService.
 * Pas besoin de la rejouer en E2E.
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
})
@DisplayName("Pastell E2E - scenario Pastell down")
class PastellEndToEndFailureTest extends PastellEndToEndTestBase {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PastellSyncRepository pastellSyncRepository;

    @Test
    @DisplayName("Pastell down : la reservation reste valide, le PastellSync est en EN_RETRY")
    void pastell_down_does_not_break_reservation() {
        // Arrange : Pastell repond 500
        wireMock.stubFor(post(urlPathEqualTo("/api/v2/entite/1/document"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"error\",\"error_message\":\"internal\"}")));

        // Act : on cree une reservation
        ReservationRequestDTO request = buildReservationRequest();
        ReservationResponseDTO response = reservationService.creerReservation(request, null);

        // Assert 1 : la reservation a bien ete creee malgre l'echec Pastell.
        // Depuis le lot signature, la creation place le dossier en EN_ATTENTE
        // (pas CONFIRMEE directement) : un admin doit valider avant confirmation.
        // Le principe "Spring autorite, Pastell satellite" est toujours garanti :
        // la panne Pastell n'a pas empeche la creation de la reservation.
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getCodeConfirmation()).isNotBlank();
        assertThat(response.getStatut().toString()).isEqualTo("EN_ATTENTE");

        // Assert 2 : Pastell a bien ete appele (l'integration n'a pas ete skip)
        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/v2/entite/1/document")));

        // Assert 3 : le PastellSync est en EN_RETRY pour reprise ulterieure (Lot 4)
        Optional<PastellSync> syncOpt = pastellSyncRepository.findByReservationId(response.getId());
        assertThat(syncOpt).isPresent();
        PastellSync sync = syncOpt.get();
        assertThat(sync.getSyncStatus()).isEqualTo(SyncStatus.EN_RETRY);
        assertThat(sync.getPastellDocumentId()).isNull();
        assertThat(sync.getTentatives()).isEqualTo(1);
        assertThat(sync.getDerniereErreur())
                .as("derniereErreur doit contenir le statut HTTP pour faciliter le diagnostic")
                .contains("[500]");
    }
}