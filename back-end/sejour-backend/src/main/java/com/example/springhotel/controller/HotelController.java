package com.example.springhotel.controller;

import com.example.springhotel.entity.Hotel;
import com.example.springhotel.repository.HotelRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/hotels")
@CrossOrigin(origins = "*")
public class HotelController {

    private final HotelRepository hotelRepository;

    public HotelController(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    // 🔹 GET — Tous les hôtels
    @GetMapping
    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }

    // 🔹 GET — Hôtel par ID
    @GetMapping("/{id}")
    public Optional<Hotel> getHotelById(@PathVariable Long id) {
        return hotelRepository.findById(id);
    }

    // 🔹 POST — CRÉER un hôtel (JSON uniquement, sans image)
    @PostMapping(consumes = "application/json")
    public Hotel createHotelJson(@RequestBody Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    // 🔹 POST — CRÉER un hôtel avec image (multipart/form-data)
    @PostMapping(consumes = "multipart/form-data")
    public Hotel createHotelWithImage(
            @RequestParam String nom,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String description,
            @RequestParam double noteMoyenne,
            @RequestParam(required = false) MultipartFile image
    ) throws Exception {

        Hotel hotel = new Hotel();
        hotel.setNom(nom);
        hotel.setAdresse(adresse);
        hotel.setVille(ville);
        hotel.setDescription(description);
        hotel.setNoteMoyenne(noteMoyenne);

        if (image != null && !image.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path uploadDir = Paths.get("src/main/resources/static/uploads/hotels");

            Files.createDirectories(uploadDir);

            Path filePath = uploadDir.resolve(fileName);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            hotel.setImageUrl("/uploads/hotels/" + fileName);
        }

        return hotelRepository.save(hotel);
    }

    // 🔹 PUT — MODIFIER un hôtel (JSON uniquement, sans image)
    @PutMapping("/{id}")
    public Hotel updateHotelJson(
            @PathVariable Long id,
            @RequestBody Hotel hotelData
    ) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel introuvable"));

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

        if (hotelData.getImageUrl() != null && !hotelData.getImageUrl().isEmpty()) {
            hotel.setImageUrl(hotelData.getImageUrl());
        }

        return hotelRepository.save(hotel);
    }

    // 🔹 POST — MODIFIER un hôtel avec image (multipart/form-data)
    @PostMapping(value = "/{id}", consumes = "multipart/form-data")
    public Hotel updateHotelWithImage(
            @PathVariable Long id,
            @RequestParam String nom,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String description,
            @RequestParam double noteMoyenne,
            @RequestParam(required = false) MultipartFile image
    ) throws Exception {

        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel introuvable"));

        hotel.setNom(nom);
        hotel.setAdresse(adresse);
        hotel.setVille(ville);
        hotel.setDescription(description);
        hotel.setNoteMoyenne(noteMoyenne);

        if (image != null && !image.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path uploadDir = Paths.get("src/main/resources/static/uploads/hotels");

            Files.createDirectories(uploadDir);

            Path filePath = uploadDir.resolve(fileName);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            hotel.setImageUrl("/uploads/hotels/" + fileName);
        }

        return hotelRepository.save(hotel);
    }

    // 🔹 DELETE — Supprimer hôtel + image
    @DeleteMapping("/{id}")
    public void deleteHotel(@PathVariable Long id) throws Exception {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel introuvable"));

        if (hotel.getImageUrl() != null && !hotel.getImageUrl().startsWith("http")) {
            Path imagePath = Paths.get("src/main/resources/static" + hotel.getImageUrl());
            Files.deleteIfExists(imagePath);
        }

        hotelRepository.delete(hotel);
    }
}