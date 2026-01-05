package com.example.springhotel.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibiliteRequestDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long hotelId; // Optionnel : filtrer par hôtel
}