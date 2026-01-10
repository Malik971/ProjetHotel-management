package com.example.springhotel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
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
    @Column(nullable = false)
    private Double prixTotal;

    // Statut de la réservation
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    // Code de confirmation unique
    @Column(unique = true)
    private String codeConfirmation;

    // Relation avec Chambre (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id", nullable = false)
    @JsonIgnore
    private Chambre chambre;

    // Relation avec User (ManyToOne) - Optionnel si l'utilisateur n'est pas connecté
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

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