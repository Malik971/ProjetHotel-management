package com.example.springhotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibiliteDTO {
    private Long chambreId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long hotelId;
    private boolean disponible;
    private LocalDate prochaineDisponibilite;
    private String message;

    public DisponibiliteDTO(Long chambreId, LocalDate dateArrivee, LocalDate dateDepart, boolean disponible, LocalDate prochaineDisponibilite, String message) {
    }
}