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

    /**
     * URLs des images de l'hotel (liens externes, ex: Unsplash).
     * Remplace l'ancien champ imageUrl (chemin local, supprime en V7).
     * La premiere URL est utilisee comme image principale dans les cartes.
     */
    @ElementCollection
    @CollectionTable(name = "hotel_images", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "image_url", length = 1000)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

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
    private Integer categorie;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Chambre> chambres = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    public void addChambre(Chambre chambre) {
        chambres.add(chambre);
        chambre.setHotel(this);
    }

    public void removeChambre(Chambre chambre) {
        chambres.remove(chambre);
        chambre.setHotel(null);
    }
}