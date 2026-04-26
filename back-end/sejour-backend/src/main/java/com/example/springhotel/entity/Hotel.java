package com.example.springhotel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hotels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String ville;
    private String adresse;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "note_moyenne")
    private Double noteMoyenne;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @ElementCollection
    @CollectionTable(name = "hotel_equipements", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "equipement")
    private List<String> equipements;

    @Column(name = "prix_moyen_nuit")
    private Double prixMoyenNuit;

    @Column(name = "categorie")
    private Integer categorie; // 3, 4, 5 étoiles

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