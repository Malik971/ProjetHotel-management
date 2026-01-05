package com.example.springhotel.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dates de réservation
    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    // Informations client
    @Column(nullable = false)
    private String nomClient;

    @Column(nullable = false)
    private String emailClient;

    @Column(nullable = false)
    private String telephoneClient;

    // Nombre de personnes (peut différer de la capacité max de la chambre)
    @Column(nullable = false)
    private Integer nombrePersonnes;

    // Prix total de la réservation (calculé selon le nombre de nuits)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixTotal;

    // Statut de la réservation
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    // Relation avec Chambre (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id", nullable = false)
    private Chambre chambre;

    // Métadonnées
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    // Enum pour le statut de réservation
    public enum StatutReservation {
        EN_ATTENTE,      // Réservation en attente de confirmation
        CONFIRMEE,       // Réservation confirmée
        ANNULEE,         // Réservation annulée
        TERMINEE         // Séjour terminé
    }
}