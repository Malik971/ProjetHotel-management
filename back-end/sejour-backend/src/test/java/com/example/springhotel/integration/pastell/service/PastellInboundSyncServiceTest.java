package com.example.springhotel.integration.pastell.service;

import com.example.springhotel.integration.pastell.client.PastellApiException;
import com.example.springhotel.integration.pastell.client.PastellClient;
import com.example.springhotel.integration.pastell.client.PastellJournalEntry;
import com.example.springhotel.integration.pastell.entity.PastellPollingCursor;
import com.example.springhotel.integration.pastell.repository.PastellPollingCursorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du {@link PastellInboundSyncService}, focus sur l'orchestration.
 *<p>
 * Le traitement individuel des entrees est externalise dans
 * {@link PastellJournalEntryProcessor} et a son propre test dedie. Ici, on
 * verifie uniquement la coordination :
 *   - Lecture du curseur
 *   - Appel du client
 *   - Iteration sur les entrees
 *   - Avancement du curseur
 *   - Resilience face aux erreurs
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PastellInboundSyncService - orchestration du polling")
class PastellInboundSyncServiceTest {

    @Mock private PastellClient pastellClient;
    @Mock private PastellPollingCursorRepository cursorRepository;
    @Mock private PastellJournalEntryProcessor entryProcessor;

    @InjectMocks private PastellInboundSyncService service;

    @Test
    @DisplayName("Aucune nouvelle entree : pas d'appel processor, lastPolledAt rafraichi")
    void runPollOnce_no_entries_only_updates_timestamp() {
        when(cursorRepository.findCursor()).thenReturn(Optional.of(buildCursor(42L)));
        when(pastellClient.fetchJournalSince(42L)).thenReturn(Collections.emptyList());

        int traites = service.runPollOnce();

        assertThat(traites).isZero();
        verify(pastellClient).fetchJournalSince(42L);
        verify(entryProcessor, never()).processEntry(any());

        // Le curseur est sauve (lastPolledAt a jour) avec le meme idJ
        ArgumentCaptor<PastellPollingCursor> captor = ArgumentCaptor.forClass(PastellPollingCursor.class);
        verify(cursorRepository).save(captor.capture());
        assertThat(captor.getValue().getLastProcessedIdJ()).isEqualTo(42L);
        assertThat(captor.getValue().getLastPolledAt()).isNotNull();
    }

    @Test
    @DisplayName("Echec HTTP du fetch : on sort sans modifier le curseur")
    void runPollOnce_http_failure_does_not_advance_cursor() {
        when(cursorRepository.findCursor()).thenReturn(Optional.of(buildCursor(10L)));
        when(pastellClient.fetchJournalSince(10L))
                .thenThrow(new PastellApiException(503, "down", "Service Unavailable"));

        int traites = service.runPollOnce();

        assertThat(traites).isZero();
        verify(entryProcessor, never()).processEntry(any());
        // Aucun save sur le curseur : on n'a meme pas eu de reponse
        verify(cursorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Curseur absent (cas H2 sans Flyway) : le poll demarre a 0")
    void runPollOnce_absent_cursor_starts_at_zero() {
        when(cursorRepository.findCursor()).thenReturn(Optional.empty());
        when(pastellClient.fetchJournalSince(0L)).thenReturn(Collections.emptyList());

        service.runPollOnce();

        verify(pastellClient).fetchJournalSince(0L);
    }

    @Test
    @DisplayName("Plusieurs entrees : le curseur avance au max idJ et le processor est appele pour chacune")
    void runPollOnce_advances_cursor_and_calls_processor_per_entry() {
        when(cursorRepository.findCursor()).thenReturn(Optional.of(buildCursor(0L)));

        List<PastellJournalEntry> entries = List.of(
                buildEntry(5, "doc-A", "creation"),
                buildEntry(7, "doc-A", "validee"),
                buildEntry(9, "doc-B", "annulee")
        );
        when(pastellClient.fetchJournalSince(0L)).thenReturn(entries);

        int traites = service.runPollOnce();

        assertThat(traites).isEqualTo(3);
        verify(entryProcessor, times(3)).processEntry(any());

        // Le curseur a ete sauve avec lastProcessedIdJ=9
        ArgumentCaptor<PastellPollingCursor> captor = ArgumentCaptor.forClass(PastellPollingCursor.class);
        verify(cursorRepository).save(captor.capture());
        assertThat(captor.getValue().getLastProcessedIdJ()).isEqualTo(9L);
    }

    @Test
    @DisplayName("Une entree leve une exception : les autres sont quand meme traitees, curseur avance quand meme")
    void runPollOnce_continues_after_one_entry_failure() {
        when(cursorRepository.findCursor()).thenReturn(Optional.of(buildCursor(0L)));

        List<PastellJournalEntry> entries = List.of(
                buildEntry(10, "doc-A", "validee"),
                buildEntry(20, "doc-B", "annulee"),
                buildEntry(30, "doc-C", "terminee")
        );
        when(pastellClient.fetchJournalSince(0L)).thenReturn(entries);

        // Stub conditionnel via doAnswer : on inspecte l'argument et on ne leve
        // QUE pour idJ=20. Pourquoi pas un doThrow(...).when(...).processEntry(entries.get(1)) ?
        // Parce que Mockito en mode strict (defaut JUnit 5) raise une PotentialStubbingProblem
        // quand on appelle processEntry avec un argument different du stub configure
        // (ici idJ=10 ou idJ=30 ne matchent pas le stub configure pour idJ=20).
        // Cette exception serait attrapee par le try/catch du code de prod et fausserait
        // le compteur 'traites'. doAnswer avec any() resout le probleme proprement
        // sans avoir a recourir a lenient() qui masquerait l'intention.
        doAnswer(invocation -> {
            PastellJournalEntry e = invocation.getArgument(0);
            if (e.idJ() == 20L) {
                throw new RuntimeException("incident sur idJ=20");
            }
            return null;
        }).when(entryProcessor).processEntry(any());

        int traites = service.runPollOnce();

        // 2 entrees traitees avec succes (les 1ere et 3eme)
        assertThat(traites).isEqualTo(2);
        verify(entryProcessor, times(3)).processEntry(any());

        // Le curseur est avance jusqu'a 30, MEME si une entree intermediaire a echoue.
        // C'est volontaire : sinon une entree cassee bloquerait tout le polling.
        ArgumentCaptor<PastellPollingCursor> captor = ArgumentCaptor.forClass(PastellPollingCursor.class);
        verify(cursorRepository).save(captor.capture());
        assertThat(captor.getValue().getLastProcessedIdJ()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Curseur cree a la volee si absent en base lors du save")
    void updateCursor_creates_row_if_missing() {
        when(cursorRepository.findCursor()).thenReturn(Optional.empty());

        service.updateCursor(99L, LocalDateTime.now());

        ArgumentCaptor<PastellPollingCursor> captor = ArgumentCaptor.forClass(PastellPollingCursor.class);
        verify(cursorRepository).save(captor.capture());
        PastellPollingCursor saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getLastProcessedIdJ()).isEqualTo(99L);
        assertThat(saved.getLastPolledAt()).isNotNull();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private PastellPollingCursor buildCursor(long lastIdJ) {
        return PastellPollingCursor.builder()
                .id(1L)
                .lastProcessedIdJ(lastIdJ)
                .lastPolledAt(LocalDateTime.now().minusMinutes(1))
                .dateCreation(LocalDateTime.now().minusDays(1))
                .dateModification(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    private PastellJournalEntry buildEntry(long idJ, String idD, String action) {
        return new PastellJournalEntry(idJ, idD, 1L, action, LocalDateTime.now());
    }
}