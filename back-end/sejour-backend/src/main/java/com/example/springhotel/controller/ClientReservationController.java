package com.example.springhotel.controller;

import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.dto.ReservationTimelineDTO;
import com.example.springhotel.dto.ReservationTimelineDTO.TimelineEtapeDTO;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.entity.Users;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.repository.ReservationRepository;
import com.example.springhotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Endpoints client pour la gestion des reservations.
 *
 * Extension Lot 2 : ajout de GET /{id}/timeline qui renvoie l'etat de
 * progression d'une reservation en vocabulaire neutre, sans mention de Pastell.
 * Le front React consomme cet endpoint pour afficher la timeline visuelle.
 *
 * Principe de securite constant dans ce controller : on verifie toujours que
 * la reservation appartient a l'utilisateur connecte via le JWT. Jamais de
 * parametre userId dans l'URL : c'est l'Authentication qui fait foi.
 */
@RestController
@RequestMapping("/api/client/reservations")
@RequiredArgsConstructor
public class ClientReservationController {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final PastellSyncRepository pastellSyncRepository;

    @GetMapping("/mes-reservations")
    public ResponseEntity<List<ReservationResponseDTO>> getMesReservations(Authentication authentication) {
        String usersEmail = authentication.getName();
        Users users = userRepository.findByEmail(usersEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        List<Reservation> reservations = reservationRepository
                .findByUsersIdOrderByDateDebutDesc(users.getId());

        List<ReservationResponseDTO> response = reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDTO> getReservationById(
            @PathVariable Long id,
            Authentication authentication) {
        String usersEmail = authentication.getName();
        Users users = userRepository.findByEmail(usersEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation non trouvee"));

        if (!reservation.getUsers().getId().equals(users.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(convertToDTO(reservation));
    }

    /**
     * Renvoie la progression d'une reservation sous forme de timeline
     * a quatre etapes en vocabulaire neutre.
     * <p>
     * Si le PastellSync n'existe pas encore (Pastell pas encore appele),
     * on renvoie quand meme la timeline avec l'etape 1 DONE et l'etape 2
     * CURRENT : l'utilisateur voit "en cours de traitement" plutot qu'une
     * erreur 404 (option A choisie lors de la conception du lot 2).
     *
     * @param id             identifiant de la reservation
     * @param authentication fournie par JwtAuthenticationFilter
     * @return ReservationTimelineDTO avec les 4 etapes ordonnees
     */
    @GetMapping("/{id}/timeline")
    public ResponseEntity<ReservationTimelineDTO> getTimeline(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        Reservation reservation = reservationRepository.findById(id)
                .orElse(null);

        if (reservation == null) {
            return ResponseEntity.notFound().build();
        }

        if (!reservation.getUsers().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        // Recuperation du PastellSync : peut etre absent si Pastell n'a pas encore
        // ete contacte (ex : cold start Render, premier appel en queue async).
        Optional<PastellSync> syncOpt = pastellSyncRepository.findByReservationId(id);

        List<TimelineEtapeDTO> etapes = buildTimeline(reservation, syncOpt.orElse(null));

        return ResponseEntity.ok(new ReservationTimelineDTO(
                reservation.getId(),
                reservation.getStatut().name(),
                etapes
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> annulerReservation(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();
        Users users = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation non trouvee"));

        if (!reservation.getUsers().getId().equals(users.getId())) {
            return ResponseEntity.status(403).build();
        }

        reservation.setStatut(Reservation.StatutReservation.ANNULEE);
        reservationRepository.save(reservation);

        return ResponseEntity.noContent().build();
    }

    /**
     * Construit la liste des 4 etapes de la timeline a partir du statut
     * de la reservation et de son PastellSync.
     * <p>
     * Table de mapping :
     * <pre>
     * Statut          | Etape1 | Etape2  | Etape3  | Etape4
     * EN_ATTENTE      | DONE   | CURRENT | PENDING | PENDING
     * CONFIRMEE       | DONE   | DONE    | CURRENT | PENDING
     * TERMINEE        | DONE   | DONE    | DONE    | DONE
     * ANNULEE         | DONE   | ERROR   | PENDING | PENDING
     * PastellSync absent => EN_ATTENTE par defaut (option A)
     * SyncStatus EN_ERREUR/DIVERGENCE => force etape 2 en ERROR
     * </pre>
     */
    private List<TimelineEtapeDTO> buildTimeline(
            Reservation reservation,
            PastellSync sync
    ) {
        Reservation.StatutReservation statut = reservation.getStatut();
        boolean syncEnErreur = sync != null && (
                sync.getSyncStatus() == SyncStatus.EN_ERREUR
                        || sync.getSyncStatus() == SyncStatus.DIVERGENCE
        );

        // Date de creation de la reservation pour l'etape 1
        Instant dateCreation = reservation.getDateCreation() != null
                ? reservation.getDateCreation().toInstant(ZoneOffset.UTC)
                : null;

        // Date de derniere synchro pour les etapes intermediaires
        Instant dateSynchro = (sync != null && sync.getDerniereSynchro() != null)
                ? sync.getDerniereSynchro().toInstant(ZoneOffset.UTC)
                : null;

        List<TimelineEtapeDTO> etapes = new ArrayList<>();

        // Etape 1 : toujours DONE, la reservation est enregistree
        etapes.add(new TimelineEtapeDTO(
                1,
                "Reservation enregistree",
                "DONE",
                dateCreation
        ));

        // Etape 2 : validation administrative
        String etape2Statut;
        Instant etape2Date = null;
        if (syncEnErreur || statut == Reservation.StatutReservation.ANNULEE) {
            etape2Statut = "ERROR";
        } else if (statut == Reservation.StatutReservation.CONFIRMEE
                || statut == Reservation.StatutReservation.TERMINEE) {
            etape2Statut = "DONE";
            etape2Date = dateSynchro;
        } else {
            // EN_ATTENTE ou PastellSync absent : en cours
            etape2Statut = "CURRENT";
        }
        etapes.add(new TimelineEtapeDTO(
                2,
                "En cours de validation",
                etape2Statut,
                etape2Date
        ));

        // Etape 3 : confirmation
        String etape3Statut;
        Instant etape3Date = null;
        if (statut == Reservation.StatutReservation.TERMINEE) {
            etape3Statut = "DONE";
            etape3Date = dateSynchro;
        } else if (statut == Reservation.StatutReservation.CONFIRMEE) {
            etape3Statut = "CURRENT";
        } else {
            etape3Statut = "PENDING";
        }
        etapes.add(new TimelineEtapeDTO(
                3,
                "Confirmee",
                etape3Statut,
                etape3Date
        ));

        // Etape 4 : sejour termine
        String etape4Statut = statut == Reservation.StatutReservation.TERMINEE
                ? "DONE"
                : "PENDING";
        Instant etape4Date = statut == Reservation.StatutReservation.TERMINEE
                ? dateSynchro
                : null;
        etapes.add(new TimelineEtapeDTO(
                4,
                "Sejour termine",
                etape4Statut,
                etape4Date
        ));

        return etapes;
    }

    private ReservationResponseDTO convertToDTO(Reservation reservation) {
        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .userId(reservation.getUsers() != null
                        ? reservation.getUsers().getId()
                        : null)
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