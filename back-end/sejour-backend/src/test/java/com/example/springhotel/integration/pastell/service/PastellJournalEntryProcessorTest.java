package com.example.springhotel.integration.pastell.service;

import com.example.springhotel.entity.Reservation;
import com.example.springhotel.entity.Reservation.StatutReservation;
import com.example.springhotel.integration.pastell.client.PastellJournalEntry;
import com.example.springhotel.integration.pastell.config.PastellProperties;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.policy.PastellActionMapper;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires Mockito du {@link PastellJournalEntryProcessor}.
 *<p>
 * Strategie : on mocke les repositories et on utilise un VRAI {@link PastellActionMapper}
 * (sans etat, sans dependance, autant tester l'integration). On verifie le comportement
 * du processor en isolation totale, sans Spring, sans BDD, sans HTTP.
 *<p>
 * Couvre tous les chemins logiques :
 *   - bascule de statut (annulee, terminee)
 *   - alignement (statut deja egal a la cible)
 *   - action neutre (creation, validee, confirmee)
 *   - action inconnue (forward compat)
 *   - filtrage par entite
 *   - PastellSync introuvable (idD inconnu)
 *   - Reservation introuvable (PastellSync orphelin)
 *   - conflit metier (DIVERGENCE)
 *   - sortie de DIVERGENCE par alignement
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PastellJournalEntryProcessor - traitement transactionnel d'une entree")
class PastellJournalEntryProcessorTest {

    @Mock private PastellSyncRepository pastellSyncRepository;
    @Mock private ReservationRepository reservationRepository;

    private PastellActionMapper actionMapper;
    private PastellProperties properties;
    private PastellJournalEntryProcessor processor;

    @BeforeEach
    void setUp() {
        actionMapper = new PastellActionMapper();
        properties = new PastellProperties();
        properties.setEntiteId(1L);
        processor = new PastellJournalEntryProcessor(
                pastellSyncRepository, reservationRepository, actionMapper, properties);
    }

    @Test
    @DisplayName("'annulee' sur reservation CONFIRMEE : bascule en ANNULEE et update PastellSync")
    void processEntry_annulee_on_confirmee_basculates_status() {
        PastellSync sync = buildSync("doc-1", 100L);
        Reservation reservation = buildReservation(100L, StatutReservation.CONFIRMEE);
        when(pastellSyncRepository.findByPastellDocumentId("doc-1")).thenReturn(Optional.of(sync));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        processor.processEntry(buildEntry(50, "doc-1", "annulee"));

        // La reservation a ete sauvegardee avec le nouveau statut
        ArgumentCaptor<Reservation> resCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(resCaptor.capture());
        assertThat(resCaptor.getValue().getStatut()).isEqualTo(StatutReservation.ANNULEE);

        // Le sync a ete sauvegarde avec etat connu = "annulee"
        ArgumentCaptor<PastellSync> syncCaptor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository).save(syncCaptor.capture());
        assertThat(syncCaptor.getValue().getPastellEtatDernierConnu()).isEqualTo("annulee");
        assertThat(syncCaptor.getValue().getDerniereSynchro()).isNotNull();
    }

    @Test
    @DisplayName("'terminee' sur reservation CONFIRMEE : bascule en TERMINEE (option A)")
    void processEntry_terminee_on_confirmee_basculates() {
        PastellSync sync = buildSync("doc-2", 101L);
        Reservation reservation = buildReservation(101L, StatutReservation.CONFIRMEE);
        when(pastellSyncRepository.findByPastellDocumentId("doc-2")).thenReturn(Optional.of(sync));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));

        processor.processEntry(buildEntry(60, "doc-2", "terminee"));

        ArgumentCaptor<Reservation> resCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(resCaptor.capture());
        assertThat(resCaptor.getValue().getStatut()).isEqualTo(StatutReservation.TERMINEE);
    }

    @Test
    @DisplayName("'validee' (action neutre) : pas de bascule, juste rafraichir l'etat connu")
    void processEntry_validee_neutre_only_updates_etat_connu() {
        PastellSync sync = buildSync("doc-3", 102L);
        Reservation reservation = buildReservation(102L, StatutReservation.CONFIRMEE);
        when(pastellSyncRepository.findByPastellDocumentId("doc-3")).thenReturn(Optional.of(sync));
        when(reservationRepository.findById(102L)).thenReturn(Optional.of(reservation));

        processor.processEntry(buildEntry(70, "doc-3", "validee"));

        verify(reservationRepository, never()).save(any());

        ArgumentCaptor<PastellSync> syncCaptor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository).save(syncCaptor.capture());
        assertThat(syncCaptor.getValue().getPastellEtatDernierConnu()).isEqualTo("validee");
    }

    @Test
    @DisplayName("Statut deja aligne ('annulee' sur ANNULEE) : pas de bascule, juste rafraichir")
    void processEntry_aligned_status_does_not_resave_reservation() {
        PastellSync sync = buildSync("doc-4", 103L);
        Reservation reservation = buildReservation(103L, StatutReservation.ANNULEE);
        when(pastellSyncRepository.findByPastellDocumentId("doc-4")).thenReturn(Optional.of(sync));
        when(reservationRepository.findById(103L)).thenReturn(Optional.of(reservation));

        processor.processEntry(buildEntry(80, "doc-4", "annulee"));

        verify(reservationRepository, never()).save(any());
        verify(pastellSyncRepository).save(any());
    }

    @Test
    @DisplayName("Conflit metier : 'annulee' Pastell sur Sejour TERMINEE -> SyncStatus DIVERGENCE, statut Sejour intact")
    void processEntry_conflict_marks_DIVERGENCE_without_changing_reservation() {
        PastellSync sync = buildSync("doc-5", 104L);
        sync.setSyncStatus(SyncStatus.OK);
        Reservation reservation = buildReservation(104L, StatutReservation.TERMINEE);
        when(pastellSyncRepository.findByPastellDocumentId("doc-5")).thenReturn(Optional.of(sync));
        when(reservationRepository.findById(104L)).thenReturn(Optional.of(reservation));

        processor.processEntry(buildEntry(90, "doc-5", "annulee"));

        verify(reservationRepository, never()).save(any());

        ArgumentCaptor<PastellSync> syncCaptor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository).save(syncCaptor.capture());
        assertThat(syncCaptor.getValue().getSyncStatus()).isEqualTo(SyncStatus.DIVERGENCE);
        assertThat(syncCaptor.getValue().getPastellEtatDernierConnu()).isEqualTo("annulee");
    }

    @Test
    @DisplayName("idD inconnu cote Sejour : log WARN et on saute, pas d'exception")
    void processEntry_unknown_idD_is_skipped_without_exception() {
        when(pastellSyncRepository.findByPastellDocumentId("doc-fantome")).thenReturn(Optional.empty());

        processor.processEntry(buildEntry(99, "doc-fantome", "annulee"));

        verify(reservationRepository, never()).save(any());
        verify(pastellSyncRepository, never()).save(any());
    }

    @Test
    @DisplayName("idEntite different de la config : entree ignoree, repos non consultes")
    void processEntry_other_entite_is_skipped() {
        PastellJournalEntry entry = new PastellJournalEntry(
                10, "doc-other", 99L /* entite differente */, "annulee", LocalDateTime.now());

        processor.processEntry(entry);

        // Garde par entite faite avant la lecture du sync
        verify(pastellSyncRepository, never()).findByPastellDocumentId(any());
        verify(reservationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("PastellSync existe mais reservation introuvable : log WARN et on saute")
    void processEntry_sync_without_reservation_is_skipped() {
        PastellSync sync = buildSync("doc-orphelin", 200L);
        when(pastellSyncRepository.findByPastellDocumentId("doc-orphelin")).thenReturn(Optional.of(sync));
        when(reservationRepository.findById(200L)).thenReturn(Optional.empty());

        processor.processEntry(buildEntry(11, "doc-orphelin", "annulee"));

        verify(reservationRepository, never()).save(any());
        verify(pastellSyncRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sortie de DIVERGENCE : sync DIVERGENCE + statut TERMINEE + action 'terminee' alignant -> retour OK")
    void processEntry_exit_divergence_when_aligned() {
        PastellSync sync = buildSync("doc-divergent", 300L);
        sync.setSyncStatus(SyncStatus.DIVERGENCE);
        Reservation reservation = buildReservation(300L, StatutReservation.TERMINEE);
        when(pastellSyncRepository.findByPastellDocumentId("doc-divergent")).thenReturn(Optional.of(sync));
        when(reservationRepository.findById(300L)).thenReturn(Optional.of(reservation));

        processor.processEntry(buildEntry(120, "doc-divergent", "terminee"));

        // Reservation deja en TERMINEE, pas de re-save
        verify(reservationRepository, never()).save(any());

        // Le sync est sorti de DIVERGENCE
        ArgumentCaptor<PastellSync> syncCaptor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository).save(syncCaptor.capture());
        assertThat(syncCaptor.getValue().getSyncStatus()).isEqualTo(SyncStatus.OK);
    }

    @Test
    @DisplayName("Action inconnue ('modifie') : ignoree quant au statut, mais etat connu rafraichi")
    void processEntry_unknown_action_only_updates_etat_connu() {
        PastellSync sync = buildSync("doc-unk", 400L);
        Reservation reservation = buildReservation(400L, StatutReservation.CONFIRMEE);
        when(pastellSyncRepository.findByPastellDocumentId("doc-unk")).thenReturn(Optional.of(sync));
        when(reservationRepository.findById(400L)).thenReturn(Optional.of(reservation));

        processor.processEntry(buildEntry(150, "doc-unk", "modifie"));

        verify(reservationRepository, never()).save(any());
        ArgumentCaptor<PastellSync> syncCaptor = ArgumentCaptor.forClass(PastellSync.class);
        verify(pastellSyncRepository).save(syncCaptor.capture());
        assertThat(syncCaptor.getValue().getPastellEtatDernierConnu()).isEqualTo("modifie");
    }

    // ============================================================
    // Helpers
    // ============================================================

    private PastellJournalEntry buildEntry(long idJ, String idD, String action) {
        return new PastellJournalEntry(idJ, idD, 1L, action, LocalDateTime.now());
    }

    private PastellSync buildSync(String idD, Long reservationId) {
        return PastellSync.builder()
                .id(System.nanoTime())
                .reservationId(reservationId)
                .pastellDocumentId(idD)
                .syncStatus(SyncStatus.OK)
                .tentatives(1)
                .build();
    }

    private Reservation buildReservation(Long id, StatutReservation statut) {
        return Reservation.builder()
                .id(id)
                .statut(statut)
                .build();
    }
}