package com.example.springhotel.integration.pastell.service;

import com.example.springhotel.entity.Reservation;
import com.example.springhotel.integration.pastell.client.PastellApiException;
import com.example.springhotel.integration.pastell.client.PastellClient;
import com.example.springhotel.integration.pastell.client.PastellCreateDocumentResponse;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires purs du PastellSyncService.
 *
 * Pourquoi pas @SpringBootTest ?
 *   - Le service ne depend que de PastellClient, PastellSyncRepository et
 *     ReservationRepository : trois dependances faciles a mocker.
 *   - Demarrage instantane : 8 tests en moins d'une seconde.
 *   - Focus chirurgical : chaque test cible une branche precise.
 *   - Cette approche est complementaire des tests d'integration WireMock du
 *     PastellClient (couverture HTTP) et des tests end-to-end du Paquet 4
 *     (couverture event -> service -> persistance).
 *
 * Strategie :
 *   - Mockito @InjectMocks construit un PastellSyncService avec les 3 mocks injectes.
 *   - Chaque test configure les mocks pour une branche precise et verifie
 *     les interactions et l'etat final attendu.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PastellSyncService - tests unitaires")
class PastellSyncServiceTest {

    @Mock
    private PastellClient pastellClient;

    @Mock
    private PastellSyncRepository pastellSyncRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private PastellSyncService pastellSyncService;

    private static final Long RESERVATION_ID = 42L;
    private static final String ID_D = "abc-123-xyz";

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
        reservation.setId(RESERVATION_ID);
    }

    @Test
    @DisplayName("skip si un PastellSync existe deja pour la reservation (idempotence)")
    void skip_when_sync_already_exists() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(true);

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        // Aucun appel HTTP n'a ete fait, aucune persistance en base
        verify(pastellClient, never()).createDocument();
        verify(pastellSyncRepository, never()).save(any());
        verify(reservationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("abandonne proprement si la reservation n'existe pas en base")
    void abandons_when_reservation_not_found() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        // Pas d'appel HTTP, pas de persistance d'un PastellSync orphelin
        verify(pastellClient, never()).createDocument();
        verify(pastellSyncRepository, never()).save(any());
    }

    @Test
    @DisplayName("persiste un PastellSync en PENDING avant l'appel Pastell")
    void persists_pending_before_calling_pastell() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellClient.createDocument())
                .thenReturn(new PastellCreateDocumentResponse(ID_D));

        // Capture l'etat du PastellSync au moment EXACT du premier save().
        // Astuce indispensable : ArgumentCaptor enregistre la reference de l'objet,
        // pas son etat. Comme le service mute le meme objet PastellSync entre les
        // deux save(), un captor classique verrait l'etat final (OK) sur les deux
        // captures. On contourne en faisant une "photo" de syncStatus a chaque save.
        AtomicReference<SyncStatus> statusAtFirstSave = new AtomicReference<>();
        AtomicReference<String> idDAtFirstSave = new AtomicReference<>();
        AtomicInteger saveCount = new AtomicInteger(0);

        when(pastellSyncRepository.save(any(PastellSync.class))).thenAnswer(invocation -> {
            PastellSync arg = invocation.getArgument(0);
            if (saveCount.getAndIncrement() == 0) {
                // Premiere persistance : on prend une photo de l'etat
                statusAtFirstSave.set(arg.getSyncStatus());
                idDAtFirstSave.set(arg.getPastellDocumentId());
            }
            return arg;
        });

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        // Au moment du premier save(), le sync DOIT etre en PENDING sans idD
        assertThat(statusAtFirstSave.get()).isEqualTo(SyncStatus.PENDING);
        assertThat(idDAtFirstSave.get()).isNull();

        // Et il y a bien eu deux save() au total (PENDING puis OK)
        assertThat(saveCount.get()).isEqualTo(2);

        // Verification d'ordre : le premier save() arrive AVANT l'appel HTTP
        InOrder inOrder = inOrder(pastellSyncRepository, pastellClient);
        inOrder.verify(pastellSyncRepository).save(any(PastellSync.class));
        inOrder.verify(pastellClient).createDocument();
    }

    @Test
    @DisplayName("succes : bascule le PastellSync en OK avec l'id_d recu")
    void success_updates_sync_to_OK_with_idD() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocument())
                .thenReturn(new PastellCreateDocumentResponse(ID_D));

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(2)).save(captor.capture());

        // Etat final apres le second save (succes)
        PastellSync finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getSyncStatus()).isEqualTo(SyncStatus.OK);
        assertThat(finalSave.getPastellDocumentId()).isEqualTo(ID_D);
        assertThat(finalSave.getTentatives()).isEqualTo(1);
        assertThat(finalSave.getDerniereSynchro()).isNotNull();
        assertThat(finalSave.getDerniereErreur()).isNull();
    }

    @Test
    @DisplayName("echec HTTP 4xx : bascule le PastellSync en EN_RETRY avec details d'erreur")
    void http_error_4xx_updates_sync_to_EN_RETRY() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocument())
                .thenThrow(new PastellApiException(401, "{\"error\":\"unauthorized\"}",
                        "Pastell a repondu en erreur HTTP 401"));

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(2)).save(captor.capture());

        PastellSync finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getSyncStatus()).isEqualTo(SyncStatus.EN_RETRY);
        assertThat(finalSave.getPastellDocumentId()).isNull();
        assertThat(finalSave.getTentatives()).isEqualTo(1);
        assertThat(finalSave.getDerniereSynchro()).isNotNull();
        assertThat(finalSave.getDerniereErreur())
                .contains("[401]")
                .contains("Pastell a repondu en erreur HTTP 401");
    }

    @Test
    @DisplayName("echec HTTP 5xx : bascule le PastellSync en EN_RETRY")
    void http_error_5xx_updates_sync_to_EN_RETRY() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocument())
                .thenThrow(new PastellApiException(500, "{\"error\":\"internal\"}",
                        "Pastell a repondu en erreur HTTP 500"));

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(2)).save(captor.capture());

        PastellSync finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getSyncStatus()).isEqualTo(SyncStatus.EN_RETRY);
        assertThat(finalSave.getDerniereErreur()).contains("[500]");
    }

    @Test
    @DisplayName("echec reseau (timeout, DNS) : bascule le PastellSync en EN_RETRY avec prefixe NETWORK")
    void network_error_updates_sync_to_EN_RETRY_with_network_prefix() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocument())
                .thenThrow(new PastellApiException(
                        "Echec d'acces a Pastell (timeout ou reseau)",
                        new RuntimeException("Connection timed out")));

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(2)).save(captor.capture());

        PastellSync finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getSyncStatus()).isEqualTo(SyncStatus.EN_RETRY);
        // Prefixe [NETWORK] au lieu de [statusCode] : permet de distinguer
        // les erreurs reseau des erreurs HTTP dans la colonne derniere_erreur
        assertThat(finalSave.getDerniereErreur())
                .contains("[NETWORK]")
                .contains("timeout");
    }

    @Test
    @DisplayName("ne propage JAMAIS d'exception meme en cas d'echec Pastell (Spring autorite, Pastell satellite)")
    void never_propagates_exception_on_pastell_failure() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocument())
                .thenThrow(new PastellApiException(503, "down", "Pastell down"));

        // Si la methode propageait l'exception, AssertJ catcherait dans assertThatCode.
        // Le test passe si aucune exception n'est levee : la reservation reste valide,
        // le PastellSync est en EN_RETRY pour reprise ulterieure.
        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        // Le sync est bien persiste en EN_RETRY
        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getSyncStatus()).isEqualTo(SyncStatus.EN_RETRY);
    }
}