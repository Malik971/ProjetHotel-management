package com.example.springhotel.dto;

import com.example.springhotel.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponseDTO {

    private Long id;
    private Long userId;
    private Long chambreId;
    private String chambreNom;
    private Long hotelId;
    private String hotelNom;
    private String hotelVille;
    private String hotelImageUrl;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String nomClient;
    private String emailClient;
    private String telephoneClient;
    private Integer nombrePersonnes;
    private BigDecimal prixTotal;
    private Reservation.StatutReservation statut;
    private String codeConfirmation;

    // Méthodes utilitaires pour compatibilité
    public LocalDate getDateArrivee() {
        return dateDebut;
    }

    public LocalDate getDateDepart() {
        return dateFin;
    }

    public Double getPrixTotalDouble() {
        return prixTotal != null ? prixTotal.doubleValue() : 0.0;
    }

    public void setPrixTotalDouble(Double prix) {
        this.prixTotal = prix != null ? BigDecimal.valueOf(prix) : BigDecimal.ZERO;
    }
}