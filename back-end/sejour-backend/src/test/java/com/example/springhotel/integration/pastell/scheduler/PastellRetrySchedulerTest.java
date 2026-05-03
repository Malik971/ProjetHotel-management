package com.example.springhotel.integration.pastell.scheduler;

import com.example.springhotel.integration.pastell.config.PastellProperties;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.policy.PastellRetryPolicy;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.integration.pastell.service.PastellSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PastellRetrySchedulerTest {

    @Mock private PastellSyncRepository pastellSyncRepository;
    @Mock private PastellSyncService pastellSyncService;
    @Mock private PastellRetryPolicy retryPolicy;

    private PastellProperties properties;
    private PastellRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new PastellProperties();
        properties.setEnabled(true);
        properties.getRetry().setSchedulerBatchSize(2);
        properties.getRetry().setMaxTentativesTotal(5);
        scheduler = new PastellRetryScheduler(pastellSyncRepository, pastellSyncService, retryPolicy, properties);
    }

    @Test
    void aucunCandidat_neFaitRien() {
        when(pastellSyncRepository.findCandidatsRetraitement(any(Collection.class), any(Pageable.class)))
                .thenReturn(List.of());

        int traites = scheduler.runRetryBatch();

        assertThat(traites).isZero();
        verifyNoInteractions(pastellSyncService);
    }

    @Test
    void plusieursCandidatsRetryables_appelleServicePourChacun() {
        PastellSync s1 = sync(1L, SyncStatus.EN_RETRY, 1, "[503] down");
        PastellSync s2 = sync(2L, SyncStatus.PENDING, 0, null);
        when(pastellSyncRepository.findCandidatsRetraitement(any(Collection.class), any(Pageable.class)))
                .thenReturn(List.of(s1, s2));
        when(retryPolicy.isRetryable("[503] down")).thenReturn(true);

        int traites = scheduler.runRetryBatch();

        assertThat(traites).isEqualTo(2);
        verify(pastellSyncService).retraiterSync(1L);
        verify(pastellSyncService).retraiterSync(2L);
    }

    @Test
    void candidatAvecQuotaMaxAtteint_basculeEnErreurSansAppelService() {
        // tentatives=5, max=5 : on bascule sans demander a retraiterSync
        PastellSync s = sync(1L, SyncStatus.EN_RETRY, 5, "[503] down");
        when(pastellSyncRepository.findCandidatsRetraitement(any(Collection.class), any(Pageable.class)))
                .thenReturn(List.of(s));
        when(pastellSyncRepository.findById(1L)).thenReturn(java.util.Optional.of(s));
        when(pastellSyncRepository.save(any(PastellSync.class))).thenAnswer(inv -> inv.getArgument(0));

        int traites = scheduler.runRetryBatch();

        assertThat(traites).isZero();
        verify(pastellSyncService, never()).retraiterSync(anyLong());

        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository).save(captor.capture());
        assertThat(captor.getValue().getSyncStatus()).isEqualTo(SyncStatus.EN_ERREUR);
    }

    @Test
    void candidatAvecErreurNonRetryable_basculeEnErreur() {
        PastellSync s = sync(1L, SyncStatus.EN_RETRY, 1, "[401] bad creds");
        when(pastellSyncRepository.findCandidatsRetraitement(any(Collection.class), any(Pageable.class)))
                .thenReturn(List.of(s));
        when(pastellSyncRepository.findById(1L)).thenReturn(java.util.Optional.of(s));
        when(pastellSyncRepository.save(any(PastellSync.class))).thenAnswer(inv -> inv.getArgument(0));
        when(retryPolicy.isRetryable("[401] bad creds")).thenReturn(false);

        int traites = scheduler.runRetryBatch();

        assertThat(traites).isZero();
        verify(pastellSyncService, never()).retraiterSync(anyLong());
        ArgumentCaptor<PastellSync> captor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository).save(captor.capture());
        assertThat(captor.getValue().getSyncStatus()).isEqualTo(SyncStatus.EN_ERREUR);
    }

    @Test
    void exceptionDansUnSync_neBloquePasLesAutres() {
        PastellSync s1 = sync(1L, SyncStatus.EN_RETRY, 1, "[503] down");
        PastellSync s2 = sync(2L, SyncStatus.EN_RETRY, 1, "[503] down");
        when(pastellSyncRepository.findCandidatsRetraitement(any(Collection.class), any(Pageable.class)))
                .thenReturn(List.of(s1, s2));
        when(retryPolicy.isRetryable("[503] down")).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(pastellSyncService).retraiterSync(1L);

        int traites = scheduler.runRetryBatch();

        // s1 a echoue (capture par le filet de securite), s2 a quand meme ete traite.
        assertThat(traites).isEqualTo(1);
        verify(pastellSyncService).retraiterSync(1L);
        verify(pastellSyncService).retraiterSync(2L);
    }

    private static PastellSync sync(Long id, SyncStatus status, int tentatives, String erreur) {
        return PastellSync.builder()
                .id(id)
                .reservationId(id * 10)
                .syncStatus(status)
                .tentatives(tentatives)
                .derniereErreur(erreur)
                .build();
    }
}