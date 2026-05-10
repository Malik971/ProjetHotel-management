package com.example.springhotel.integration.pastell.scheduler;

import com.example.springhotel.integration.pastell.service.PastellInboundSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires Mockito du {@link PastellPollingScheduler}.
 *<p>
 * On NE TESTE PAS la planification reelle (le timer Spring tourne deja sur des
 * passes integration de Spring lui-meme, on n'a rien a y rajouter). On teste
 * uniquement que :
 *   - scheduledPoll() delegue correctement au service.
 *   - Une exception du service ne fait pas remonter d'erreur (le scheduler
 *     ne doit jamais s'arreter a cause d'un incident isole).
 *<p>
 * La couverture du fonctionnement du scheduler est faite par PastellPollingEndToEndTest
 * qui demarre un vrai contexte Spring et observe le polling tourner.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PastellPollingScheduler - declenchement et resilience")
class PastellPollingSchedulerTest {

    @Mock private PastellInboundSyncService inboundSyncService;
    @InjectMocks private PastellPollingScheduler scheduler;

    @BeforeEach
    void setUp() {
        // Mockito injecte automatiquement le mock dans le scheduler via @InjectMocks.
        // Pas de logique d'init particuliere.
    }

    @Test
    @DisplayName("scheduledPoll delegue runPollOnce au service")
    void scheduledPoll_delegates_to_service() {
        when(inboundSyncService.runPollOnce()).thenReturn(0);

        scheduler.scheduledPoll();

        verify(inboundSyncService, times(1)).runPollOnce();
    }

    @Test
    @DisplayName("scheduledPoll attrape les exceptions du service (resilience du scheduler)")
    void scheduledPoll_swallows_exceptions() {
        // Si le service leve une RuntimeException inattendue, le scheduler doit
        // logger et ne pas remonter, sinon Spring pourrait arreter de planifier
        // les tics suivants.
        when(inboundSyncService.runPollOnce())
                .thenThrow(new RuntimeException("incident inattendu"));

        // Le scheduler ne doit JAMAIS propager
        assertThatCode(() -> scheduler.scheduledPoll()).doesNotThrowAnyException();

        verify(inboundSyncService, times(1)).runPollOnce();
    }

    @Test
    @DisplayName("scheduledPoll peut etre appele plusieurs fois sans effet de bord (idempotence du tick)")
    void scheduledPoll_can_be_called_repeatedly() {
        when(inboundSyncService.runPollOnce()).thenReturn(0);

        scheduler.scheduledPoll();
        scheduler.scheduledPoll();
        scheduler.scheduledPoll();

        verify(inboundSyncService, times(3)).runPollOnce();
    }
}