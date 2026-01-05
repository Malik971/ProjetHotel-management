package com.example.springhotel.service;

import com.example.springhotel.dto.ChambreDTO;
import com.example.springhotel.dto.DisponibiliteRequestDTO;
import com.example.springhotel.entity.Chambre;
import com.example.springhotel.entity.Hotel;
import com.example.springhotel.exception.ResourceNotFoundException;
import com.example.springhotel.repository.ChambreRepository;
import com.example.springhotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChambreService {

    private final ChambreRepository chambreRepository;
    private final HotelRepository hotelRepository;

    // Mapper : Entity -> DTO
    private ChambreDTO toDTO(Chambre chambre) {
        return ChambreDTO.builder()
                .id(chambre.getId())
                .nom(chambre.getNom())
                .prixParNuit(chambre.getPrixParNuit())
                .capacite(chambre.getCapacite())
                .superficie(chambre.getSuperficie())
                .typeLit(chambre.getTypeLit())
                .description(chambre.getDescription())
                .equipements(chambre.getEquipements())
                .imageUrls(chambre.getImageUrls())
                .hotelId(chambre.getHotel().getId())
                .hotelNom(chambre.getHotel().getNom())
                .build();
    }

    // Mapper : DTO -> Entity
    private Chambre toEntity(ChambreDTO dto) {
        Hotel hotel = hotelRepository.findById(dto.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hôtel non trouvé avec l'ID : " + dto.getHotelId()));

        return Chambre.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .prixParNuit(dto.getPrixParNuit())
                .capacite(dto.getCapacite())
                .superficie(dto.getSuperficie())
                .typeLit(dto.getTypeLit())
                .description(dto.getDescription())
                .equipements(dto.getEquipements())
                .imageUrls(dto.getImageUrls())
                .hotel(hotel)
                .build();
    }

    // CREATE
    @Transactional
    public ChambreDTO creerChambre(ChambreDTO chambreDTO) {
        Chambre chambre = toEntity(chambreDTO);
        Chambre saved = chambreRepository.save(chambre);
        return toDTO(saved);
    }

    // READ ALL
    public List<ChambreDTO> getAllChambres() {
        return chambreRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // READ BY ID
    public ChambreDTO getChambreById(Long id) {
        Chambre chambre = chambreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre non trouvée avec l'ID : " + id));
        return toDTO(chambre);
    }

    // READ BY HOTEL
    public List<ChambreDTO> getChambresByHotel(Long hotelId) {
        return chambreRepository.findByHotelId(hotelId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // UPDATE
    @Transactional
    public ChambreDTO updateChambre(Long id, ChambreDTO chambreDTO) {
        Chambre existante = chambreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre non trouvée avec l'ID : " + id));

        // Mise à jour des champs
        existante.setNom(chambreDTO.getNom());
        existante.setPrixParNuit(chambreDTO.getPrixParNuit());
        existante.setCapacite(chambreDTO.getCapacite());
        existante.setSuperficie(chambreDTO.getSuperficie());
        existante.setTypeLit(chambreDTO.getTypeLit());
        existante.setDescription(chambreDTO.getDescription());
        existante.setEquipements(chambreDTO.getEquipements());
        existante.setImageUrls(chambreDTO.getImageUrls());

        // Mise à jour de l'hôtel si changé
        if (!existante.getHotel().getId().equals(chambreDTO.getHotelId())) {
            Hotel nouvelHotel = hotelRepository.findById(chambreDTO.getHotelId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hôtel non trouvé avec l'ID : " + chambreDTO.getHotelId()));
            existante.setHotel(nouvelHotel);
        }

        Chambre updated = chambreRepository.save(existante);
        return toDTO(updated);
    }

    // DELETE
    @Transactional
    public void deleteChambre(Long id) {
        if (!chambreRepository.existsById(id)) {
            throw new ResourceNotFoundException("Chambre non trouvée avec l'ID : " + id);
        }
        chambreRepository.deleteById(id);
    }

    // DISPONIBILITÉ
    public List<ChambreDTO> getChambresDisponibles(DisponibiliteRequestDTO request) {
        List<Chambre> chambres;

        if (request.getHotelId() != null) {
            // Filtrer par hôtel
            chambres = chambreRepository.findChambresDisponiblesParHotel(
                    request.getHotelId(),
                    request.getDateDebut(),
                    request.getDateFin()
            );
        } else {
            // Toutes les chambres disponibles
            chambres = chambreRepository.findChambresDisponibles(
                    request.getDateDebut(),
                    request.getDateFin()
            );
        }

        return chambres.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}