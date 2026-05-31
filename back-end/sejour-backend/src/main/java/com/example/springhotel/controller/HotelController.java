package com.example.springhotel.controller;

import com.example.springhotel.entity.Hotel;
import com.example.springhotel.repository.HotelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller CRUD pour les hotels.
 *
 * Evolution V7 : suppression du systeme d'upload de fichiers.
 * Les images sont desormais des URLs externes (Unsplash, CDN, etc.)
 * stockees dans la table hotel_images via Hotel.imageUrls.
 *
 * Tous les endpoints acceptent et retournent du JSON uniquement.
 * Le multipart/form-data est supprime : plus de fichiers sur le serveur.
 */
@RestController
@RequestMapping("/api/hotels")
@CrossOrigin(origins = "*")
public class HotelController {

    private final HotelRepository hotelRepository;

    public HotelController(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    /** GET /api/hotels — Tous les hotels */
    @GetMapping
    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }

    /** GET /api/hotels/{id} — Hotel par ID */
    @GetMapping("/{id}")
    public ResponseEntity<Hotel> getHotelById(@PathVariable Long id) {
        return hotelRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/hotels — Creer un hotel (JSON) */
    @PostMapping
    public ResponseEntity<Hotel> createHotel(@RequestBody Hotel hotel) {
        Hotel saved = hotelRepository.save(hotel);
        return ResponseEntity.status(201).body(saved);
    }

    /** PUT /api/hotels/{id} — Modifier un hotel (JSON) */
    @PutMapping("/{id}")
    public ResponseEntity<Hotel> updateHotel(
            @PathVariable Long id,
            @RequestBody Hotel hotelData
    ) {
        return hotelRepository.findById(id)
                .map(hotel -> {
                    hotel.setNom(hotelData.getNom());
                    hotel.setAdresse(hotelData.getAdresse());
                    hotel.setVille(hotelData.getVille());
                    hotel.setDescription(hotelData.getDescription());
                    hotel.setNoteMoyenne(hotelData.getNoteMoyenne());
                    hotel.setLatitude(hotelData.getLatitude());
                    hotel.setLongitude(hotelData.getLongitude());
                    hotel.setPrixMoyenNuit(hotelData.getPrixMoyenNuit());
                    hotel.setCategorie(hotelData.getCategorie());
                    if (hotelData.getEquipements() != null) {
                        hotel.setEquipements(hotelData.getEquipements());
                    }
                    // Mise a jour des URLs d'images (liste complete en remplacement)
                    if (hotelData.getImageUrls() != null) {
                        hotel.getImageUrls().clear();
                        hotel.getImageUrls().addAll(hotelData.getImageUrls());
                    }
                    return ResponseEntity.ok(hotelRepository.save(hotel));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** DELETE /api/hotels/{id} — Supprimer un hotel */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        if (!hotelRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hotelRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}