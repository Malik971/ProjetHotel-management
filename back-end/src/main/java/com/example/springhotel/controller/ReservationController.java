package com.example.springhotel.controller;

import com.example.springhotel.dto.ReservationRequestDTO;
import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponseDTO> creerReservation(
            @RequestBody ReservationRequestDTO request,
            Authentication authentication
    ) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ReservationResponseDTO reservation = reservationService.creerReservation(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }
}