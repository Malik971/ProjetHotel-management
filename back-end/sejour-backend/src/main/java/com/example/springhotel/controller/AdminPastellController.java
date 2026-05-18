package com.example.springhotel.controller;

import com.example.springhotel.dto.*;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.integration.pastell.config.PastellProperties;
import com.example.springhotel.integration.pastell.entity.PastellJournalEntryRecord;
import com.example.springhotel.integration.pastell.entity.PastellPollingCursor;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.repository.PastellJournalEntryRecordRepository;
import com.example.springhotel.integration.pastell.repository.PastellPollingCursorRepository;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.integration.pastell.service.PastellInboundSyncService;
import com.example.springhotel.integration.pastell.service.PastellSyncService;
import com.example.springhotel.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller admin pour la demonstration et le diagnostic de l'integration Pastell.
 * <p>
 * <b>Evolution Lot 3 (admin Pastell complet) :</b>
 *   - {@code GET /api/admin/pastell-sync} : page de dossiers (PagedResponseDTO)
 *   - {@code GET /api/admin/pastell-sync/{syncId}/journal} : journal d'un dossier
 *   - {@code POST /api/admin/pastell-sync/{syncId}/retry} : relance manuelle
 *   - {@code GET /api/admin/activity} : flux d'activite recente
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminPastellController {

    private static final Logger log = LoggerFactory.getLogger(AdminPastellController.class);

    private final ReservationRepository reservationRepository;
    private final PastellSyncRepository pastellSyncRepository;
    private final PastellPollingCursorRepository cursorRepository;
    private final PastellJournalEntryRecordRepository journalRecordRepository;
    private final ObjectProvider<PastellInboundSyncService> inboundSyncServiceProvider;
    private final ObjectProvider<PastellSyncService> syncServiceProvider;
    private final ObjectProvider<PastellProperties> pastellPropertiesProvider;

    @Value("${demo.admin-token:}")
    private String demoAdminToken;

    private final HttpClient pingClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public AdminPastellController(
            ReservationRepository reservationRepository,
            PastellSyncRepository pastellSyncRepository,
            PastellPollingCursorRepository cursorRepository,
            PastellJournalEntryRecordRepository journalRecordRepository,
            ObjectProvider<PastellInboundSyncService> inboundSyncServiceProvider,
            ObjectProvider<PastellSyncService> syncServiceProvider,
            ObjectProvider<PastellProperties> pastellPropertiesProvider) {
        this.reservationRepository = reservationRepository;
        this.pastellSyncRepository = pastellSyncRepository;
        this.cursorRepository = cursorRepository;
        this.journalRecordRepository = journalRecordRepository;
        this.inboundSyncServiceProvider = inboundSyncServiceProvider;
        this.syncServiceProvider = syncServiceProvider;
        this.pastellPropertiesProvider = pastellPropertiesProvider;
    }

    // ============================================================
    // ENDPOINTS CONSERVES DU LOT 6
    // ============================================================

    @GetMapping("/reservations/{id}")
    public ResponseEntity<ReservationResponseDTO> getReservation(@PathVariable Long id) {
        return reservationRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pastell-sync/reservation/{reservationId}")
    public ResponseEntity<PastellSync> getPastellSyncByReservation(@PathVariable Long reservationId) {
        return pastellSyncRepository.findByReservationId(reservationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pastell/cursor")
    public ResponseEntity<PastellPollingCursor> getCursor() {
        return cursorRepository.findCursor()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/pastell/poll")
    public ResponseEntity<Map<String, Object>> forcePoll(
            @RequestHeader(value = "X-Demo-Token", required = false) String providedToken) {

        if (!isDemoTokenValid(providedToken)) {
            log.warn("forcePoll : X-Demo-Token absent ou invalide.");
            Map<String, Object> body = new HashMap<>();
            body.put("error", "forbidden");
            body.put("hint", "Header X-Demo-Token requis pour cette operation.");
            return ResponseEntity.status(403).body(body);
        }

        PastellInboundSyncService service = inboundSyncServiceProvider.getIfAvailable();
        if (service == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("error", "Pastell integration disabled");
            body.put("hint", "Set pastell.enabled=true to use this endpoint");
            return ResponseEntity.status(503).body(body);
        }
        int processed = service.runPollOnce();
        Map<String, Object> response = new HashMap<>();
        response.put("processed", processed);
        response.put("polledAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pastell/status")
    public ResponseEntity<PastellStatusDTO> getStatus() {
        PastellProperties props = pastellPropertiesProvider.getIfAvailable();
        boolean enabled = props != null && props.isEnabled();

        PastellStatusDTO.PastellStatusDTOBuilder builder = PastellStatusDTO.builder()
                .generatedAt(LocalDateTime.now())
                .pastellEnabled(enabled)
                .syncCountOk(pastellSyncRepository.countBySyncStatus(SyncStatus.OK))
                .syncCountPending(pastellSyncRepository.countBySyncStatus(SyncStatus.PENDING))
                .syncCountEnRetry(pastellSyncRepository.countBySyncStatus(SyncStatus.EN_RETRY))
                .syncCountEnErreur(pastellSyncRepository.countBySyncStatus(SyncStatus.EN_ERREUR))
                .syncCountDivergence(pastellSyncRepository.countBySyncStatus(SyncStatus.DIVERGENCE))
                .reservationCount(reservationRepository.count());

        cursorRepository.findCursor().ifPresent(cursor -> {
            builder.lastProcessedIdJ(cursor.getLastProcessedIdJ());
            builder.lastPolledAt(cursor.getLastPolledAt());
        });

        builder.mockHealth(pingMock(props));

        return ResponseEntity.ok(builder.build());
    }

    // ============================================================
    // NOUVEAUX ENDPOINTS LOT 3
    // ============================================================

    /**
     * Page de PastellSync, jointe avec les infos de reservation pour le tableau admin.
     * <p>
     * Renvoie un {@link PagedResponseDTO} qui contient le contenu de la page et
     * les metadonnees (totalElements, totalPages, first, last). Cela permet au
     * front d'afficher une vraie pagination numerotee sans appel supplementaire.
     *
     * @param status statut a filtrer (optionnel, null = tous les statuts)
     * @param page   numero de page demande, 0-based
     * @param size   taille de page
     * @return page de dossiers, jamais null
     */
    @GetMapping("/pastell-sync")
    public ResponseEntity<PagedResponseDTO<PastellSyncSummaryDTO>> listAllSyncs(
            @RequestParam(required = false) SyncStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(
                page, size,
                Sort.by(Sort.Direction.DESC, "derniereSynchro")
        );

        Page<PastellSync> syncPage = (status != null)
                ? pastellSyncRepository.findBySyncStatusOrderByDerniereSynchroDesc(status, pageRequest)
                : pastellSyncRepository.findAll(pageRequest);

        Page<PastellSyncSummaryDTO> dtoPage = syncPage.map(this::toSummaryDto);

        return ResponseEntity.ok(PagedResponseDTO.from(dtoPage));
    }

    /**
     * Journal complet d'un dossier (toutes les entrees PastellJournalEntryRecord
     * liees a son document Pastell), ordonne par date croissante.
     */
    @GetMapping("/pastell-sync/{syncId}/journal")
    public ResponseEntity<List<PastellJournalEntryDTO>> getSyncJournal(@PathVariable Long syncId) {
        Optional<PastellSync> syncOpt = pastellSyncRepository.findById(syncId);
        if (syncOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PastellSync sync = syncOpt.get();
        String pastellDocId = sync.getPastellDocumentId();

        List<PastellJournalEntryRecord> entries = pastellDocId != null
                ? journalRecordRepository.findByPastellDocumentIdOrderByOccurredAtAsc(pastellDocId)
                : Collections.emptyList();

        List<PastellJournalEntryDTO> dtos = entries.stream()
                .map(this::toJournalDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Force la relance d'un sync specifique. Reset son statut et reprogramme
     * un envoi vers Pastell. Protege par X-Demo-Token.
     */
    @PostMapping("/pastell-sync/{syncId}/retry")
    public ResponseEntity<Map<String, Object>> retrySync(
            @PathVariable Long syncId,
            @RequestHeader(value = "X-Demo-Token", required = false) String providedToken) {

        if (!isDemoTokenValid(providedToken)) {
            Map<String, Object> body = new HashMap<>();
            body.put("error", "forbidden");
            body.put("hint", "Header X-Demo-Token requis pour relancer un dossier.");
            return ResponseEntity.status(403).body(body);
        }

        Optional<PastellSync> syncOpt = pastellSyncRepository.findById(syncId);
        if (syncOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PastellSync sync = syncOpt.get();
        sync.setSyncStatus(SyncStatus.EN_RETRY);
        sync.setTentatives(0);
        sync.setDerniereErreur(null);
        pastellSyncRepository.save(sync);

        PastellSyncService syncService = syncServiceProvider.getIfAvailable();
        boolean triggered = false;
        if (syncService != null) {
            try {
                syncService.retraiterSync(sync.getId());
                triggered = true;
            } catch (Exception e) {
                log.warn("Relance immediate echouee, le scheduler reprendra : {}", e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("syncId", syncId);
        response.put("newStatus", SyncStatus.EN_RETRY.name());
        response.put("triggered", triggered);
        response.put("retriedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Flux d'activite recente pour le dashboard.
     */
    @GetMapping("/activity")
    public ResponseEntity<List<ActivityEntryDTO>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {

        PageRequest pageRequest = PageRequest.of(0, limit);
        List<PastellJournalEntryRecord> entries = journalRecordRepository.findAllRecent(pageRequest);

        List<ActivityEntryDTO> activity = new ArrayList<>();
        for (PastellJournalEntryRecord entry : entries) {
            Optional<PastellSync> syncOpt = entry.getPastellDocumentId() != null
                    ? pastellSyncRepository.findByPastellDocumentId(entry.getPastellDocumentId())
                    : Optional.empty();

            Long reservationId = syncOpt.map(PastellSync::getReservationId).orElse(null);
            String clientName = "";
            String hotelName = "";

            if (reservationId != null) {
                Reservation res = reservationRepository.findById(reservationId).orElse(null);
                if (res != null) {
                    clientName = res.getNomClient() != null ? res.getNomClient() : "";
                    if (res.getChambre() != null && res.getChambre().getHotel() != null) {
                        hotelName = res.getChambre().getHotel().getNom();
                    }
                }
            }

            ActivityEntryDTO act = ActivityEntryDTO.builder()
                    .type(deriveActivityType(entry))
                    .title(deriveActivityTitle(entry, reservationId))
                    .subtitle(clientName + (hotelName.isEmpty() ? "" : " - " + hotelName))
                    .reservationId(reservationId)
                    .occurredAt(entry.getOccurredAt())
                    .build();
            activity.add(act);
        }

        return ResponseEntity.ok(activity);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private boolean isDemoTokenValid(String providedToken) {
        if (demoAdminToken == null || demoAdminToken.isBlank()) {
            return true;
        }
        return demoAdminToken.equals(providedToken);
    }

    private PastellSyncSummaryDTO toSummaryDto(PastellSync sync) {
        Reservation res = reservationRepository.findById(sync.getReservationId()).orElse(null);

        String clientNom = res != null ? res.getNomClient() : null;
        String clientEmail = res != null ? res.getEmailClient() : null;
        String hotelNom = (res != null && res.getChambre() != null && res.getChambre().getHotel() != null)
                ? res.getChambre().getHotel().getNom()
                : null;
        String resStatut = res != null && res.getStatut() != null ? res.getStatut().name() : null;

        return PastellSyncSummaryDTO.builder()
                .syncId(sync.getId())
                .reservationId(sync.getReservationId())
                .clientNom(clientNom)
                .clientEmail(clientEmail)
                .hotelNom(hotelNom)
                .reservationStatut(resStatut)
                .syncStatus(sync.getSyncStatus())
                .etapeCircuit(sync.getPastellEtatDernierConnu())
                .pastellDocumentId(sync.getPastellDocumentId())
                .derniereSynchro(sync.getDerniereSynchro())
                .retryCount(sync.getTentatives())
                .errorMessage(sync.getDerniereErreur())
                .build();
    }

    private PastellJournalEntryDTO toJournalDto(PastellJournalEntryRecord entry) {
        return PastellJournalEntryDTO.builder()
                .id(entry.getId())
                .idJ(entry.getIdJ())
                .pastellDocumentId(entry.getPastellDocumentId())
                .action(entry.getAction())
                .idEntitePastell(entry.getIdEntitePastell())
                .occurredAt(entry.getOccurredAt())
                .recordedAt(entry.getRecordedAt())
                .severity(entry.getSeverity())
                .message(entry.getMessage())
                .build();
    }

    private String deriveActivityType(PastellJournalEntryRecord entry) {
        if ("ERROR".equalsIgnoreCase(entry.getSeverity())) {
            return "EN_ERREUR";
        }
        String action = entry.getAction();
        if (action == null) {
            return "PENDING";
        }
        return switch (action.toLowerCase()) {
            case "validee", "confirmee", "terminee" -> "OK";
            case "annulee" -> "EN_ERREUR";
            case "creation" -> "PENDING";
            case "en-attente-validation" -> "PENDING";
            default -> "PENDING";
        };
    }

    private String deriveActivityTitle(PastellJournalEntryRecord entry, Long reservationId) {
        String prefix = "Dossier " + (reservationId != null ? "#" + reservationId : "");
        if (entry.getMessage() != null && !entry.getMessage().isBlank()) {
            return prefix + " : " + entry.getMessage().toLowerCase();
        }
        String action = entry.getAction();
        if (action == null) {
            return prefix + " mis a jour";
        }
        return switch (action.toLowerCase()) {
            case "creation" -> prefix + " soumis au bus Pastell";
            case "en-attente-validation" -> prefix + " transmis au parapheur";
            case "validee" -> prefix + " valide par l'agent";
            case "confirmee" -> prefix + " confirme";
            case "terminee" -> prefix + " cloture";
            case "annulee" -> prefix + " annule";
            default -> prefix + " : action " + action;
        };
    }

    private PastellStatusDTO.MockHealth pingMock(PastellProperties props) {
        if (props == null || !props.isEnabled() || props.getUrl() == null) {
            return PastellStatusDTO.MockHealth.builder()
                    .reachable(false)
                    .errorMessage("Pastell desactive ou URL absente")
                    .build();
        }
        String pingUrl = props.getUrl() + "/api/version.php";
        long start = System.nanoTime();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(pingUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<Void> resp = pingClient.send(req, HttpResponse.BodyHandlers.discarding());
            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
            return PastellStatusDTO.MockHealth.builder()
                    .reachable(resp.statusCode() >= 200 && resp.statusCode() < 300)
                    .responseTimeMs(elapsed)
                    .errorMessage(resp.statusCode() >= 300
                            ? "HTTP " + resp.statusCode()
                            : null)
                    .build();
        } catch (Exception e) {
            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
            return PastellStatusDTO.MockHealth.builder()
                    .reachable(false)
                    .responseTimeMs(elapsed)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private ReservationResponseDTO toDto(Reservation reservation) {
        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .userId(reservation.getUsers() != null ? reservation.getUsers().getId() : null)
                .chambreId(reservation.getChambre().getId())
                .chambreNom(reservation.getChambre().getNom())
                .hotelId(reservation.getChambre().getHotel().getId())
                .hotelNom(reservation.getChambre().getHotel().getNom())
                .hotelVille(reservation.getChambre().getHotel().getVille())
                .hotelImageUrl(reservation.getChambre().getHotel().getImageUrl())
                .dateDebut(reservation.getDateDebut())
                .dateFin(reservation.getDateFin())
                .nomClient(reservation.getNomClient())
                .emailClient(reservation.getEmailClient())
                .telephoneClient(reservation.getTelephoneClient())
                .nombrePersonnes(reservation.getNombrePersonnes())
                .prixTotal(reservation.getPrixTotal() != null
                        ? BigDecimal.valueOf(reservation.getPrixTotal())
                        : BigDecimal.ZERO)
                .statut(reservation.getStatut())
                .codeConfirmation(reservation.getCodeConfirmation())
                .build();
    }
}