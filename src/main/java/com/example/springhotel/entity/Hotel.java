package com.example.springhotel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String adresse;
    private String ville;
    private String description;
    private double noteMoyenne;
    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    // Relation bidirectionnelle avec Chambre
    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore  // ⚠️ IMPORTANT : Empêche la sérialisation cyclique
    private List<Chambre> chambres = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    // Méthode utilitaire pour ajouter une chambre
    public void addChambre(Chambre chambre) {
        chambres.add(chambre);
        chambre.setHotel(this);
    }

    // Méthode utilitaire pour retirer une chambre
    public void removeChambre(Chambre chambre) {
        chambres.remove(chambre);
        chambre.setHotel(null);
    }
}