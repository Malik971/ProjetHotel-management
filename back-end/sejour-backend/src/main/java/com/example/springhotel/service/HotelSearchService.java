package com.example.springhotel.service;

import com.example.springhotel.dto.HotelSearchDTO;
import com.example.springhotel.entity.Hotel;
import com.example.springhotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelSearchService {

    private final HotelRepository hotelRepository;

    public List<Hotel> searchHotels(HotelSearchDTO searchDTO) {
        List<Hotel> hotels = hotelRepository.findAll();

        // Filtrer par ville
        if (searchDTO.getVille() != null && !searchDTO.getVille().isEmpty()) {
            hotels = hotels.stream()
                    .filter(h -> h.getVille() != null &&
                            h.getVille().toLowerCase().contains(searchDTO.getVille().toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Filtrer par prix
        if (searchDTO.getPrixMin() != null) {
            hotels = hotels.stream()
                    .filter(h -> h.getPrixMoyenNuit() != null && h.getPrixMoyenNuit() >= searchDTO.getPrixMin())
                    .collect(Collectors.toList());
        }

        if (searchDTO.getPrixMax() != null) {
            hotels = hotels.stream()
                    .filter(h -> h.getPrixMoyenNuit() != null && h.getPrixMoyenNuit() <= searchDTO.getPrixMax())
                    .collect(Collectors.toList());
        }

        // Filtrer par catégorie (étoiles)
        if (searchDTO.getCategorieMin() != null) {
            hotels = hotels.stream()
                    .filter(h -> h.getCategorie() != null && h.getCategorie() >= searchDTO.getCategorieMin())
                    .collect(Collectors.toList());
        }

        // Filtrer par note moyenne
        if (searchDTO.getNoteMoyenneMin() != null) {
            hotels = hotels.stream()
                    .filter(h -> h.getNoteMoyenne() != null && h.getNoteMoyenne() >= searchDTO.getNoteMoyenneMin())
                    .collect(Collectors.toList());
        }

        // Filtrer par équipements
        if (searchDTO.getEquipements() != null && !searchDTO.getEquipements().isEmpty()) {
            hotels = hotels.stream()
                    .filter(h -> h.getEquipements() != null &&
                            h.getEquipements().containsAll(searchDTO.getEquipements()))
                    .collect(Collectors.toList());
        }

        // Filtrer par distance (si coordonnées fournies)
        if (searchDTO.getLatitude() != null && searchDTO.getLongitude() != null && searchDTO.getRayonKm() != null) {
            hotels = hotels.stream()
                    .filter(h -> {
                        if (h.getLatitude() == null || h.getLongitude() == null) return false;
                        double distance = calculateDistance(
                                searchDTO.getLatitude(), searchDTO.getLongitude(),
                                h.getLatitude(), h.getLongitude()
                        );
                        return distance <= searchDTO.getRayonKm();
                    })
                    .collect(Collectors.toList());
        }

        return hotels;
    }

    // Formule de Haversine pour calculer la distance entre deux points GPS
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon de la Terre en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}