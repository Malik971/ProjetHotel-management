package com.example.springhotel.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReservationRequestDTO {
    private Long chambreId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String nomClient;
    private String emailClient;
    private String telephoneClient;
    private Integer nombrePersonnes;
}