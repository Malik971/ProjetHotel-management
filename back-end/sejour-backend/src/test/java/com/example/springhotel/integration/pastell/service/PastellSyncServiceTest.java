package com.example.springhotel.integration.pastell.service;

import com.example.springhotel.entity.Reservation;
import com.example.springhotel.integration.pastell.client.PastellApiException;
import com.example.springhotel.integration.pastell.client.PastellClientWithRetry;
import com.example.springhotel.integration.pastell.client.PastellCreateDocumentResponse;
import com.example.springhotel.integration.pastell.config.PastellProperties;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.policy.PastellRetryPolicy;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
 * Tests unitaires purs du PastellSyncService (Lot 4).
 *
 * Pourquoi pas @SpringBootTest ?
 *   - Le service ne depend que de PastellClientWithRetry, PastellSyncRepository,
 *     ReservationRepository, PastellRetryPolicy et PastellProperties : faciles a
 *     mocker (sauf PastellProperties qu'on construit en vrai pour la lisibilite).
 *   - Demarrage instantane : 12 tests en moins d'une seconde.
 *   - Focus chirurgical : chaque test cible une branche precise.
 *
 * Strategie :
 *   - Les 4 vraies dependances sont mockees (@Mock).
 *   - PastellProperties est construite en vrai dans @BeforeEach : c'est un POJO
 *     de configuration, le mocker rajouterait du bruit sans valeur ajoutee.
 *   - Le service est construit a la main dans @BeforeEach (pas @InjectMocks)
 *     pour gerer cette dependance mixte mock/reel proprement.
 *
 * Difference avec le Lot 3 :
 *   - Le service appelle PastellClientWithRetry.createDocumentWithRetry()
 *     a la place de PastellClient.createDocument().
 *   - Une erreur non-retryable bascule directement en EN_ERREUR (avant : EN_RETRY).
 *   - Une nouvelle methode publique retraiterSync(Long) couverte par 5 tests.
 *   - Le quota maxTentativesTotal est respecte : au-dela, EN_ERREUR force.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PastellSyncService - tests unitaires (Lot 4)")
class PastellSyncServiceTest {

    @Mock
    private PastellClientWithRetry pastellClient;

    @Mock
    private PastellSyncRepository pastellSyncRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PastellRetryPolicy retryPolicy;

    private PastellProperties properties;
    private PastellSyncService pastellSyncService;

    private static final Long RESERVATION_ID = 42L;
    private static final Long SYNC_ID = 7L;
    private static final String ID_D = "abc-123-xyz";
    private static final int MAX_TENTATIVES_TOTAL = 5;

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
        reservation.setId(RESERVATION_ID);

        // Configuration retry construite a la main : c'est un POJO de config,
        // pas une dependance metier qui justifierait un mock. Garde le test lisible.
        properties = new PastellProperties();
        properties.setEnabled(true);
        properties.getRetry().setMaxTentativesTotal(MAX_TENTATIVES_TOTAL);

        pastellSyncService = new PastellSyncService(
                pastellClient,
                pastellSyncRepository,
                reservationRepository,
                retryPolicy,
                properties);
    }

    // ============================================================
    // synchroniserCreation : idempotence et garde-fou reservation
    // ============================================================

    @Test
    @DisplayName("synchroniserCreation : skip si un PastellSync existe deja (idempotence)")
    void skip_when_sync_already_exists() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(true);

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        // Aucun appel HTTP, aucune persistance, on n'a meme pas charge la reservation
        verify(pastellClient, never()).createDocumentWithRetry();
        verify(pastellSyncRepository, never()).save(any());
        verify(reservationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("synchroniserCreation : abandonne proprement si la reservation n'existe pas")
    void abandons_when_reservation_not_found() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        // Pas d'appel HTTP, pas de PastellSync orphelin persiste pour rien
        verify(pastellClient, never()).createDocumentWithRetry();
        verify(pastellSyncRepository, never()).save(any());
    }

    // ============================================================
    // synchroniserCreation : ordre PENDING -> appel HTTP -> save final
    // ============================================================

    @Test
    @DisplayName("synchroniserCreation : persiste un PastellSync en PENDING avant l'appel Pastell")
    void persists_pending_before_calling_pastell() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellClient.createDocumentWithRetry())
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
        inOrder.verify(pastellClient).createDocumentWithRetry();
    }

    // ============================================================
    // synchroniserCreation : succes
    // ============================================================

    @Test
    @DisplayName("synchroniserCreation succes : bascule le PastellSync en OK avec l'id_d recu")
    void success_updates_sync_to_OK_with_idD() {
        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocumentWithRetry())
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

    // ============================================================
    // synchroniserCreation : echec retryable -> EN_RETRY
    // ============================================================

    @Test
    @DisplayName("synchroniserCreation echec HTTP 5xx (retryable) : bascule en EN_RETRY")
    void http_error_5xx_retryable_updates_sync_to_EN_RETRY() {
        PastellApiException erreur = new PastellApiException(500, "{\"error\":\"internal\"}",
                "Pastell a repondu en erreur HTTP 500");

        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocumentWithRetry()).thenThrow(erreur);
        when(retryPolicy.isRetryable(erreur)).thenReturn(true);

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(2)).save(captor.capture());

        PastellSync finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getSyncStatus()).isEqualTo(SyncStatus.EN_RETRY);
        assertThat(finalSave.getPastellDocumentId()).isNull();
        assertThat(finalSave.getTentatives()).isEqualTo(1);
        assertThat(finalSave.getDerniereSynchro()).isNotNull();
        assertThat(finalSave.getDerniereErreur())
                .contains("[500]")
                .contains("Pastell a repondu en erreur HTTP 500");
    }

    @Test
    @DisplayName("synchroniserCreation echec reseau (retryable) : bascule en EN_RETRY avec prefixe NETWORK")
    void network_error_retryable_updates_sync_to_EN_RETRY_with_network_prefix() {
        PastellApiException erreur = new PastellApiException(
                "Echec d'acces a Pastell (timeout ou reseau)",
                new RuntimeException("Connection timed out"));

        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocumentWithRetry()).thenThrow(erreur);
        when(retryPolicy.isRetryable(erreur)).thenReturn(true);

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(2)).save(captor.capture());

        PastellSync finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getSyncStatus()).isEqualTo(SyncStatus.EN_RETRY);
        // Prefixe [NETWORK] au lieu de [statusCode] : permet a la PastellRetryPolicy
        // de reconnaitre l'erreur reseau quand elle relit derniere_erreur
        assertThat(finalSave.getDerniereErreur())
                .contains("[NETWORK]")
                .contains("Echec d'acces a Pastell");
    }

    // ============================================================
    // synchroniserCreation : echec non-retryable -> EN_ERREUR direct (Lot 4)
    // ============================================================

    @Test
    @DisplayName("synchroniserCreation echec HTTP 401 (non-retryable) : bascule directe en EN_ERREUR")
    void http_error_401_non_retryable_updates_sync_to_EN_ERREUR_directly() {
        PastellApiException erreur = new PastellApiException(401, "{\"error\":\"unauthorized\"}",
                "Pastell a repondu en erreur HTTP 401");

        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocumentWithRetry()).thenThrow(erreur);
        when(retryPolicy.isRetryable(erreur)).thenReturn(false);

        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(2)).save(captor.capture());

        PastellSync finalSave = captor.getAllValues().get(1);
        // Difference cle avec le Lot 3 : 401 ne donne plus EN_RETRY mais EN_ERREUR
        // direct. Le scheduler ne reprendra jamais ce sync, ca evite de spammer
        // Pastell sur des erreurs incorrigibles automatiquement.
        assertThat(finalSave.getSyncStatus()).isEqualTo(SyncStatus.EN_ERREUR);
        assertThat(finalSave.getTentatives()).isEqualTo(1);
        assertThat(finalSave.getDerniereErreur()).contains("[401]");
    }

    // ============================================================
    // synchroniserCreation : ne propage jamais d'exception
    // ============================================================

    @Test
    @DisplayName("synchroniserCreation : ne propage JAMAIS d'exception (Spring autorite, Pastell satellite)")
    void never_propagates_exception_on_pastell_failure() {
        PastellApiException erreur = new PastellApiException(503, "down", "Pastell down");

        when(pastellSyncRepository.existsByReservationId(RESERVATION_ID)).thenReturn(false);
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocumentWithRetry()).thenThrow(erreur);
        when(retryPolicy.isRetryable(erreur)).thenReturn(true);

        // Si la methode propageait l'exception, ce test echouerait avec une
        // exception non capturee. Le test passe si aucune exception n'est levee :
        // la reservation reste valide en base, le PastellSync est en EN_RETRY
        // pour reprise par le scheduler.
        pastellSyncService.synchroniserCreation(RESERVATION_ID);

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getSyncStatus()).isEqualTo(SyncStatus.EN_RETRY);
    }

    // ============================================================
    // retraiterSync : nouveaux tests Lot 4
    // ============================================================

    @Test
    @DisplayName("retraiterSync : sync inexistant en base, sortie silencieuse")
    void retraiterSync_sync_introuvable_ne_fait_rien() {
        when(pastellSyncRepository.findById(SYNC_ID)).thenReturn(Optional.empty());

        pastellSyncService.retraiterSync(SYNC_ID);

        // Garde-fou : si on appelle un id qui n'existe plus (purge concurrente,
        // bug, etc.), on ne plante pas, on n'appelle pas Pastell, on ne save rien.
        verify(pastellClient, never()).createDocumentWithRetry();
        verify(pastellSyncRepository, never()).save(any());
    }

    @Test
    @DisplayName("retraiterSync : sync deja en OK, garde-fou anti-double-traitement")
    void retraiterSync_sync_OK_skip() {
        PastellSync existant = PastellSync.builder()
                .id(SYNC_ID)
                .reservationId(RESERVATION_ID)
                .syncStatus(SyncStatus.OK)
                .pastellDocumentId(ID_D)
                .tentatives(1)
                .build();
        when(pastellSyncRepository.findById(SYNC_ID)).thenReturn(Optional.of(existant));

        pastellSyncService.retraiterSync(SYNC_ID);

        // Le scheduler N'EST PAS cense selectionner les OK. Mais en defense en
        // profondeur, le service refuse aussi : un sync OK ne doit JAMAIS etre
        // retraite (sinon on creerait un deuxieme dossier Pastell pour rien).
        verify(pastellClient, never()).createDocumentWithRetry();
        verify(pastellSyncRepository, never()).save(any());
    }

    @Test
    @DisplayName("retraiterSync : sync deja en EN_ERREUR, garde-fou anti-resurrection")
    void retraiterSync_sync_EN_ERREUR_skip() {
        PastellSync existant = PastellSync.builder()
                .id(SYNC_ID)
                .reservationId(RESERVATION_ID)
                .syncStatus(SyncStatus.EN_ERREUR)
                .tentatives(MAX_TENTATIVES_TOTAL)
                .derniereErreur("[401] bad creds")
                .build();
        when(pastellSyncRepository.findById(SYNC_ID)).thenReturn(Optional.of(existant));

        pastellSyncService.retraiterSync(SYNC_ID);

        // Une fois en EN_ERREUR, c'est definitif : seul un admin peut le relancer
        // manuellement (dashboard du Lot 6). Le scheduler ne doit jamais le ramener
        // a la vie automatiquement.
        verify(pastellClient, never()).createDocumentWithRetry();
        verify(pastellSyncRepository, never()).save(any());
    }

    @Test
    @DisplayName("retraiterSync succes : EN_RETRY -> OK avec tentatives incrementees")
    void retraiterSync_succes_passe_en_OK_et_incremente_tentatives() {
        PastellSync existant = PastellSync.builder()
                .id(SYNC_ID)
                .reservationId(RESERVATION_ID)
                .syncStatus(SyncStatus.EN_RETRY)
                .tentatives(2)
                .derniereErreur("[503] down")
                .build();
        when(pastellSyncRepository.findById(SYNC_ID)).thenReturn(Optional.of(existant));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocumentWithRetry())
                .thenReturn(new PastellCreateDocumentResponse(ID_D));

        pastellSyncService.retraiterSync(SYNC_ID);

        // Une seule sauvegarde ici : pas de "PENDING avant appel" (le sync existe deja).
        // Le service met a jour le sync existant en place.
        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(1)).save(captor.capture());

        PastellSync mis = captor.getValue();
        assertThat(mis.getSyncStatus()).isEqualTo(SyncStatus.OK);
        assertThat(mis.getPastellDocumentId()).isEqualTo(ID_D);
        // tentatives passe de 2 a 3 : compteur cumule sur toutes les passes
        assertThat(mis.getTentatives()).isEqualTo(3);
        // derniereErreur est nettoye au passage en OK pour eviter de garder
        // un message trompeur sur un sync qui finalement a reussi
        assertThat(mis.getDerniereErreur()).isNull();
    }

    @Test
    @DisplayName("retraiterSync : echec retryable qui atteint maxTentativesTotal -> EN_ERREUR force")
    void retraiterSync_echec_qui_atteint_quota_max_bascule_en_EN_ERREUR() {
        // tentatives=4, max=5. L'echec courant fait passer tentatives a 5,
        // donc on bascule EN_ERREUR meme si l'erreur est retryable.
        // C'est le garde-fou anti-boucle infinie.
        PastellSync existant = PastellSync.builder()
                .id(SYNC_ID)
                .reservationId(RESERVATION_ID)
                .syncStatus(SyncStatus.EN_RETRY)
                .tentatives(MAX_TENTATIVES_TOTAL - 1)
                .derniereErreur("[503] down")
                .build();
        PastellApiException erreur = new PastellApiException(503, "down", "still down");

        when(pastellSyncRepository.findById(SYNC_ID)).thenReturn(Optional.of(existant));
        when(pastellSyncRepository.save(any(PastellSync.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pastellClient.createDocumentWithRetry()).thenThrow(erreur);
        when(retryPolicy.isRetryable(erreur)).thenReturn(true);

        pastellSyncService.retraiterSync(SYNC_ID);

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository, times(1)).save(captor.capture());

        PastellSync mis = captor.getValue();
        // Bascule definitive en EN_ERREUR par exhaustion du quota
        assertThat(mis.getSyncStatus()).isEqualTo(SyncStatus.EN_ERREUR);
        assertThat(mis.getTentatives()).isEqualTo(MAX_TENTATIVES_TOTAL);
        assertThat(mis.getDerniereErreur()).contains("[503]");
    }
}