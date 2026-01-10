package com.example.springhotel.controller;

import com.example.springhotel.dto.ChambreDTO;
import com.example.springhotel.dto.DisponibiliteDTO;
import com.example.springhotel.service.ChambreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chambres")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // À adapter selon votre config
public class ChambreController {

    private final ChambreService chambreService;

    /**
     * CREATE - Créer une nouvelle chambre
     * POST /api/chambres
     */
    @PostMapping
    public ResponseEntity<ChambreDTO> creerChambre(@RequestBody ChambreDTO chambreDTO) {
        ChambreDTO created = chambreService.creerChambre(chambreDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * READ ALL - Récupérer toutes les chambres
     * GET /api/chambres
     */
    @GetMapping
    public ResponseEntity<List<ChambreDTO>> getAllChambres() {
        List<ChambreDTO> chambres = chambreService.getAllChambres();
        return ResponseEntity.ok(chambres);
    }

    /**
     * READ BY ID - Récupérer une chambre par ID
     * GET /api/chambres/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChambreDTO> getChambreById(@PathVariable Long id) {
        ChambreDTO chambre = chambreService.getChambreById(id);
        return ResponseEntity.ok(chambre);
    }

    /**
     * READ BY HOTEL - Récupérer les chambres d'un hôtel
     * GET /api/chambres/hotel/{hotelId}
     */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<ChambreDTO>> getChambresByHotel(@PathVariable Long hotelId) {
        List<ChambreDTO> chambres = chambreService.getChambresByHotel(hotelId);
        return ResponseEntity.ok(chambres);
    }

    /**
     * UPDATE - Modifier une chambre existante
     * PUT /api/chambres/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChambreDTO> updateChambre(
            @PathVariable Long id,
            @RequestBody ChambreDTO chambreDTO
    ) {
        ChambreDTO updated = chambreService.updateChambre(id, chambreDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE - Supprimer une chambre
     * DELETE /api/chambres/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChambre(@PathVariable Long id) {
        chambreService.deleteChambre(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DISPONIBILITÉ - Récupérer les chambres disponibles entre deux dates
     * GET /api/chambres/disponibles?dateDebut=YYYY-MM-DD&dateFin=YYYY-MM-DD&hotelId=X
     */
    @GetMapping("/disponibles")
    public ResponseEntity<List<ChambreDTO>> getChambresDisponibles(
            @RequestParam String dateDebut,
            @RequestParam String dateFin,
            @RequestParam(required = false) Long hotelId
    ) {
        DisponibiliteDTO request = new DisponibiliteDTO();
        request.setDateDebut(java.time.LocalDate.parse(dateDebut));
        request.setDateFin(java.time.LocalDate.parse(dateFin));
        request.setHotelId(hotelId);

        List<ChambreDTO> chambres = chambreService.getChambresDisponibles(request);
        return ResponseEntity.ok(chambres);
    }
}