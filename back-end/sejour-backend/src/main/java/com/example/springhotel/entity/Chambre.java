package com.example.springhotel.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chambre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Informations de base
    @Column(nullable = false)
    private String nom; // Ex: "Chambre Classique", "Suite Deluxe"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixParNuit; // Prix en euros

    @Column(nullable = false)
    private Integer capacity; // Nombre de personnes

    @Column(nullable = false)
    private Integer superficie; // En m²

    @Column(nullable = false)
    private String typeLit; // Ex: "Lit Queen", "2 Lits Simples"

    @Column(length = 1000)
    private String description; // Description détaillée (optionnelle)

    // Équipements (stockés sous forme de liste de chaînes)
    @ElementCollection
    @CollectionTable(name = "chambre_equipements", joinColumns = @JoinColumn(name = "chambre_id"))
    @Column(name = "equipement")
    @Builder.Default
    private List<String> equipment = new ArrayList<>(); // Ex: ["Climatisation", "Minibar", "TV"]

    // Images multiples (slider)
    @ElementCollection
    @CollectionTable(name = "chambre_images", joinColumns = @JoinColumn(name = "chambre_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>(); // Chemins vers les images

    // Relation avec Hotel (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    @JsonBackReference  // ✅ Ajout de cette annotation
    private Hotel hotel;

    // Relation avec Reservation (OneToMany, bidirectionnelle)
    @OneToMany(mappedBy = "chambre", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // ✅ Ajout de cette annotation
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    // Métadonnées
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    // Méthode utilitaire pour ajouter une réservation (maintien de la cohérence bidirectionnelle)
    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
        reservation.setChambre(this);
    }

    // Méthode utilitaire pour retirer une réservation
    public void removeReservation(Reservation reservation) {
        reservations.remove(reservation);
        reservation.setChambre(null);
    }

    public Object getType() {
        return null;
    }
}