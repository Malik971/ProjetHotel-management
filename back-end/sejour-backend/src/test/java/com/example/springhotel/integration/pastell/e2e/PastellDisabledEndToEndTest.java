package com.example.springhotel.integration.pastell.e2e;

import com.example.springhotel.dto.ReservationRequestDTO;
import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie que lorsque pastell.enabled=false, AUCUN appel a Pastell n'est fait
 * et AUCUN PastellSync n'est cree, meme quand une reservation est creee.
 *
 * C'est le test critique du principe "integration optionnelle, zero impact
 * quand desactivee". Si ce test echoue, c'est que la condition @ConditionalOnProperty
 * sur le ReservationCreatedListener ou le PastellSyncService ne fonctionne pas,
 * et l'application risque de faire des appels reseau meme quand l'integration
 * est desactivee.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pastell.enabled=false"
})
@DisplayName("Pastell E2E - integration desactivee")
class PastellDisabledEndToEndTest extends PastellEndToEndTestBase {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PastellSyncRepository pastellSyncRepository;

    @Test
    @DisplayName("creerReservation marche normalement, aucun appel Pastell, aucun PastellSync")
    void reservation_works_without_pastell_when_disabled() {
        // Act : on cree une reservation
        ReservationRequestDTO request = buildReservationRequest();
        ReservationResponseDTO response = reservationService.creerReservation(request, null);

        // Assert 1 : la reservation est bien creee
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getCodeConfirmation()).isNotBlank();

        // Assert 2 : AUCUN appel HTTP n'a ete fait vers WireMock.
        wireMock.verify(0, anyRequestedFor(anyUrl()));

        // Assert 3 : AUCUN PastellSync n'a ete cree.
        assertThat(pastellSyncRepository.findByReservationId(response.getId()))
                .as("aucun PastellSync ne doit etre cree quand pastell.enabled=false")
                .isEmpty();
    }
}