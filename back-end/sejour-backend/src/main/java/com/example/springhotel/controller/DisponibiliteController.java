package com.example.springhotel.controller;

import com.example.springhotel.dto.DisponibiliteDTO;
import com.example.springhotel.service.DisponibiliteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/disponibilite")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DisponibiliteController {

    private final DisponibiliteService disponibiliteService;

    /**
     * Vérifier la disponibilité simple (retourne boolean)
     * GET /api/disponibilite/check?chambreId=1&dateDebut=2024-01-01&dateFin=2024-01-05
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkDisponibilite(
            @RequestParam Long chambreId,
            @RequestParam String dateDebut,
            @RequestParam String dateFin
    ) {
        LocalDate debut = LocalDate.parse(dateDebut);
        LocalDate fin = LocalDate.parse(dateFin);

        boolean disponible = disponibiliteService.isChambreDisponible(chambreId, debut, fin);
        return ResponseEntity.ok(disponible);
    }

    /**
     * Vérifier la disponibilité avec détails
     * GET /api/disponibilite/verifier?chambreId=1&dateDebut=2024-01-01&dateFin=2024-01-05
     */
    @GetMapping("/verifier")
    public ResponseEntity<DisponibiliteDTO> verifierDisponibilite(
            @RequestParam Long chambreId,
            @RequestParam String dateDebut,
            @RequestParam String dateFin
    ) {
        LocalDate debut = LocalDate.parse(dateDebut);
        LocalDate fin = LocalDate.parse(dateFin);

        DisponibiliteDTO result = disponibiliteService.verifierDisponibilite(chambreId, debut, fin);
        return ResponseEntity.ok(result);
    }

    /**
     * Vérifier la disponibilité avec un body JSON
     * POST /api/disponibilite/verifier
     */
    @PostMapping("/verifier")
    public ResponseEntity<DisponibiliteDTO> verifierDisponibilitePost(
            @RequestBody DisponibiliteDTO request
    ) {
        DisponibiliteDTO result = disponibiliteService.verifierDisponibilite(
                request.getChambreId(),
                request.getDateDebut(),
                request.getDateFin()
        );
        return ResponseEntity.ok(result);
    }
}