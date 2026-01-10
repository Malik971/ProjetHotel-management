package com.example.springhotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelSearchDTO {
    private String ville;
    private LocalDate dateArrivee;
    private LocalDate dateDepart;
    private Integer nombrePersonnes;
    private Double prixMin;
    private Double prixMax;
    private Integer categorieMin; // Nombre d'étoiles minimum
    private List<String> equipements;
    private Double noteMoyenneMin;
    private Double latitude;
    private Double longitude;
    private Double rayonKm; // Pour la recherche par distance
}