package com.example.springhotel.controller;

import com.example.springhotel.dto.ReservationDTO;
import com.example.springhotel.entity.Reservation.StatutReservation;
import com.example.springhotel.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // À adapter selon votre config
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * CREATE - Créer une nouvelle réservation
     * POST /api/reservations
     */
    @PostMapping
    public ResponseEntity<ReservationDTO> creerReservation(@RequestBody ReservationDTO reservationDTO) {
        ReservationDTO created = reservationService.creerReservation(reservationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * READ ALL - Récupérer toutes les réservations
     * GET /api/reservations
     */
    @GetMapping
    public ResponseEntity<List<ReservationDTO>> getAllReservations() {
        List<ReservationDTO> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations);
    }

    /**
     * READ BY ID - Récupérer une réservation par ID
     * GET /api/reservations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservationById(@PathVariable Long id) {
        ReservationDTO reservation = reservationService.getReservationById(id);
        return ResponseEntity.ok(reservation);
    }

    /**
     * READ BY CHAMBRE - Récupérer les réservations d'une chambre
     * GET /api/reservations/chambre/{chambreId}
     */
    @GetMapping("/chambre/{chambreId}")
    public ResponseEntity<List<ReservationDTO>> getReservationsByChambre(@PathVariable Long chambreId) {
        List<ReservationDTO> reservations = reservationService.getReservationsByChambre(chambreId);
        return ResponseEntity.ok(reservations);
    }

    /**
     * READ BY CLIENT - Récupérer les réservations d'un client
     * GET /api/reservations/client/{email}
     */
    @GetMapping("/client/{email}")
    public ResponseEntity<List<ReservationDTO>> getReservationsByClient(@PathVariable String email) {
        List<ReservationDTO> reservations = reservationService.getReservationsByClient(email);
        return ResponseEntity.ok(reservations);
    }

    /**
     * UPDATE STATUT - Modifier le statut d'une réservation
     * PATCH /api/reservations/{id}/statut
     */
    @PatchMapping("/{id}/statut")
    public ResponseEntity<ReservationDTO> updateStatut(
            @PathVariable Long id,
            @RequestParam StatutReservation statut
    ) {
        ReservationDTO updated = reservationService.updateStatut(id, statut);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE - Annuler/Supprimer une réservation
     * DELETE /api/reservations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

}