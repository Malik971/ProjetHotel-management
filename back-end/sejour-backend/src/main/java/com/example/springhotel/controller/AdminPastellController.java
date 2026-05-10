package com.example.springhotel.controller;

import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.integration.pastell.entity.PastellPollingCursor;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.repository.PastellPollingCursorRepository;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.integration.pastell.service.PastellInboundSyncService;
import com.example.springhotel.repository.ReservationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller admin pour la demonstration et le diagnostic de l'integration Pastell.
 *<p>
 * Expose des endpoints simples qui contournent l'authentification utilisateur
 * (presents sous /api/admin/** qui est en permitAll() dans SecurityConfig).
 * Sert principalement au dashboard de demo HTML pour :
 *   - Lire une reservation par ID sans avoir besoin d'etre connecte
 *   - Decouvrir le pastellDocumentId associe a une reservation
 *   - Inspecter le curseur de polling
 *   - Forcer un poll manuel sans attendre les 30 secondes du scheduler
 *<p>
 * Pourquoi ces endpoints ne sont pas dans les controllers existants :
 *   - ClientReservationController exige une auth (Authentication parameter), il
 *     n'est pas adapte pour un dashboard de demo qui tourne sans login.
 *   - On garde la separation : les controllers metier protegent leurs endpoints,
 *     les controllers admin/debug exposent une API simplifiee.
 *<p>
 * Pourquoi ObjectProvider<PastellInboundSyncService> :
 *   - Le service est conditionnel (@ConditionalOnProperty pastell.enabled=true).
 *     Si Pastell est desactive, le bean n'existe pas et un Autowired direct
 *     ferait planter le demarrage. ObjectProvider permet une injection optionnelle :
 *     on l'utilise si present, on retourne 503 si absent.
 *<p>
 * Securite : ces endpoints sont publics par construction. C'est ACCEPTABLE pour
 * le portfolio et la demo locale, ce le serait MOINS pour la production. En prod,
 * ce controller serait protege par hasRole("ADMIN") et /api/admin/** ne serait
 * plus en permitAll().
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminPastellController {

    private final ReservationRepository reservationRepository;
    private final PastellSyncRepository pastellSyncRepository;
    private final PastellPollingCursorRepository cursorRepository;
    private final ObjectProvider<PastellInboundSyncService> inboundSyncServiceProvider;

    public AdminPastellController(
            ReservationRepository reservationRepository,
            PastellSyncRepository pastellSyncRepository,
            PastellPollingCursorRepository cursorRepository,
            ObjectProvider<PastellInboundSyncService> inboundSyncServiceProvider) {
        this.reservationRepository = reservationRepository;
        this.pastellSyncRepository = pastellSyncRepository;
        this.cursorRepository = cursorRepository;
        this.inboundSyncServiceProvider = inboundSyncServiceProvider;
    }

    /**
     * Lit une reservation par son ID, sans authentification.
     * Equivalent simplifie de ClientReservationController.getReservationById().
     */
    @GetMapping("/reservations/{id}")
    public ResponseEntity<ReservationResponseDTO> getReservation(@PathVariable Long id) {
        return reservationRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lit le PastellSync associe a une reservation. Utilise par le dashboard
     * pour auto-decouvrir le pastellDocumentId.
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
     * Pratique pour la demo : on declenche manuellement la synchronisation
     * descendante apres avoir change un etat dans Pastell.
     *<p>
     * Retourne le nombre d'entrees traitees + un timestamp.
     * Si Pastell est desactive (pastell.enabled=false), retourne 503.
     */
    @PostMapping("/pastell/poll")
    public ResponseEntity<Map<String, Object>> forcePoll() {
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

    // ============================================================
    // Conversion DTO (copie minimale de ClientReservationController.convertToDTO,
    // sans la dependance a Authentication)
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