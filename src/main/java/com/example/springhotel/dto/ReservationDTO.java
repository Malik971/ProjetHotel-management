package com.example.springhotel.dto;

import com.example.springhotel.entity.Reservation.StatutReservation;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDTO {
    private Long id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String nomClient;
    private String emailClient;
    private String telephoneClient;
    private Integer nombrePersonnes;
    private BigDecimal prixTotal;
    private StatutReservation statut;
    private Long chambreId;
    private String chambreNom; // Pour l'affichage
}