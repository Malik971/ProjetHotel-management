package com.example.springhotel.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChambreDTO {
    private Long id;
    private String nom;
    private BigDecimal prixParNuit;
    private Integer capacite;
    private Integer superficie;
    private String typeLit;
    private String description;
    private List<String> equipements;
    private List<String> imageUrls;
    private Long hotelId;
    private String hotelNom; // Pour l'affichage côté front
}