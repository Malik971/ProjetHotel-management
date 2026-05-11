package com.example.springhotel.controller;

import com.example.springhotel.dto.PastellStatusDTO;
import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.integration.pastell.config.PastellProperties;
import com.example.springhotel.integration.pastell.entity.PastellPollingCursor;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.repository.PastellPollingCursorRepository;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.integration.pastell.service.PastellInboundSyncService;
import com.example.springhotel.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller admin pour la demonstration et le diagnostic de l'integration Pastell.
 * <p>
 * <b>Evolution Lot 6 :</b>
 *   <ul>
 *     <li>Ajout de {@code GET /api/admin/pastell/status} qui retourne un snapshot
 *         de l'integration (compteurs, curseur, ping mock). Consomme par le dashboard
 *         et la page status.html.</li>
 *     <li>Le endpoint {@code POST /api/admin/pastell/poll} exige maintenant un header
 *         {@code X-Demo-Token} qui doit matcher la propriete {@code demo.admin-token}.
 *         Cette protection est legere (le token est connu du JS du dashboard) mais
 *         suffit a empecher le bruit de bots aveugles. Voir DEMO_PUBLIQUE.md.</li>
 *   </ul>
 * <p>
 * Endpoints publics par construction (under /api/admin/** en permitAll dans SecurityConfig).
 * C'est acceptable pour le portfolio et la demo. Pour une vraie prod, ce controller
 * serait protege par hasRole("ADMIN") et /api/admin/** ne serait plus en permitAll.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminPastellController {

    private static final Logger log = LoggerFactory.getLogger(AdminPastellController.class);

    private final ReservationRepository reservationRepository;
    private final PastellSyncRepository pastellSyncRepository;
    private final PastellPollingCursorRepository cursorRepository;
    private final ObjectProvider<PastellInboundSyncService> inboundSyncServiceProvider;
    private final ObjectProvider<PastellProperties> pastellPropertiesProvider;

    /**
     * Token attendu sur l'entete {@code X-Demo-Token} pour les operations
     * destructives. Vide en local (dev), defini en prod (variable d'env
     * {@code DEMO_ADMIN_TOKEN}). Si vide, la verification est court-circuitee.
     */
    @Value("${demo.admin-token:}")
    private String demoAdminToken;

    /**
     * Client HTTP a la JDK pour le ping du mock. Pas de RestClient, pas de
     * dependance sur PastellConfig (ce controller doit tourner meme si Pastell
     * est desactive, par exemple en cas de panne du mock).
     */
    private final HttpClient pingClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public AdminPastellController(
            ReservationRepository reservationRepository,
            PastellSyncRepository pastellSyncRepository,
            PastellPollingCursorRepository cursorRepository,
            ObjectProvider<PastellInboundSyncService> inboundSyncServiceProvider,
            ObjectProvider<PastellProperties> pastellPropertiesProvider) {
        this.reservationRepository = reservationRepository;
        this.pastellSyncRepository = pastellSyncRepository;
        this.cursorRepository = cursorRepository;
        this.inboundSyncServiceProvider = inboundSyncServiceProvider;
        this.pastellPropertiesProvider = pastellPropertiesProvider;
    }

    /**
     * Lit une reservation par son ID, sans authentification.
     */
    @GetMapping("/reservations/{id}")
    public ResponseEntity<ReservationResponseDTO> getReservation(@PathVariable Long id) {
        return reservationRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lit le PastellSync associe a une reservation.
     */
    @GetMapping("/pastell-sync/reservation/{reservationId}")
    public ResponseEntity<PastellSync> getPastellSyncByReservation(@PathVariable Long reservationId) {
        return pastellSyncRepository.findByReservationId(reservationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lit l'etat courant du curseur de polling.
     */
    @GetMapping("/pastell/cursor")
    public ResponseEntity<PastellPollingCursor> getCursor() {
        return cursorRepository.findCursor()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Force un tick de polling sans attendre les 30 secondes du scheduler.
     * Protege par X-Demo-Token (Lot 6). Si le token n'est pas configure, accepte.
     */
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

    /**
     * Snapshot de l'integration Pastell, Lot 6.
     * Endpoint public (pas de token requis), consomme par le dashboard et status.html.
     */
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

    /**
     * Ping HTTP du mock Pastell. Retourne un MockHealth synthese.
     */
    private PastellStatusDTO.MockHealth pingMock(PastellProperties props) {
        if (props == null || !props.isEnabled() || props.getUrl() == null) {
            return PastellStatusDTO.MockHealth.builder()
                    .reachable(false)
                    .errorMessage("Pastell desactive ou URL absente")
                    .build();
        }
        // Endpoint version.php : anonyme cote mock, ideal pour un ping sans credentials.
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
                    .errorMessage(resp.statusCode() >= 300 ? "HTTP " + resp.statusCode() : null)
                    .build();
        } catch (Exception e) {
            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
            return PastellStatusDTO.MockHealth.builder()
                    .reachable(false)
                    .responseTimeMs(elapsed)
                    .errorMessage(e.getClass().getSimpleName() + ": " + e.getMessage())
                    .build();
        }
    }

    /**
     * Verifie le X-Demo-Token. Si la propriete demo.admin-token est vide,
     * la verification est court-circuitee (dev local). En prod, la propriete
     * est obligatoirement renseignee.
     */
    private boolean isDemoTokenValid(String providedToken) {
        if (demoAdminToken == null || demoAdminToken.isBlank()) {
            return true;
        }
        return demoAdminToken.equals(providedToken);
    }

    // ============================================================
    // Conversion DTO (copie minimale, sans la dependance a Authentication)
    // ============================================================

    private ReservationResponseDTO toDto(Reservation reservation) {
        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .userId(reservation.getUsers() != null ? reservation.getUsers().getId() : null)
                .chambreId(reservation.getChambre() != null ? reservation.getChambre().getId() : null)
                .chambreNom(reservation.getChambre() != null ? reservation.getChambre().getNom() : null)
                .hotelId(reservation.getChambre() != null && reservation.getChambre().getHotel() != null
                        ? reservation.getChambre().getHotel().getId() : null)
                .hotelNom(reservation.getChambre() != null && reservation.getChambre().getHotel() != null
                        ? reservation.getChambre().getHotel().getNom() : null)
                .hotelVille(reservation.getChambre() != null && reservation.getChambre().getHotel() != null
                        ? reservation.getChambre().getHotel().getVille() : null)
                .hotelImageUrl(reservation.getChambre() != null && reservation.getChambre().getHotel() != null
                        ? reservation.getChambre().getHotel().getImageUrl() : null)
                .dateDebut(reservation.getDateDebut())
                .dateFin(reservation.getDateFin())
                .nomClient(reservation.getNomClient())
                .emailClient(reservation.getEmailClient())
                .telephoneClient(reservation.getTelephoneClient())
                .nombrePersonnes(reservation.getNombrePersonnes())
                .prixTotal(reservation.getPrixTotal() != null
                        ? BigDecimal.valueOf(reservation.getPrixTotal()) : BigDecimal.ZERO)
                .statut(reservation.getStatut())
                .codeConfirmation(reservation.getCodeConfirmation())
                .build();
    }
}
