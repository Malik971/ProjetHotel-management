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

    // Dates de reservation
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

    // Nombre de personnes
    @Column(nullable = false)
    private Integer nombrePersonnes;

    // Prix total de la reservation
    @Column(nullable = false)
    private Double prixTotal;

    // Statut de la reservation
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    // Code de confirmation unique
    @Column(unique = true)
    private String codeConfirmation;

    // -------------------------------------------------------------------
    // Champs de signature electronique.
    //
    // signaturePdfBase64 : recepisse PDF encode en base64, produit par
    //   SignatureService apres apposition. NULL jusqu'a SIGNATURE_APPOSEE.
    //   Point de migration niveau 3 : au lieu d'etre genere localement, ce
    //   PDF sera retourne par le mock parapheur via son API REST.
    //
    // nomSignataire : nom de l'agent tel que saisi sur la page de signature.
    //
    // signedAt : horodatage UTC de l'apposition. Immutable apres ecriture.
    // -------------------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String signaturePdfBase64;

    @Column(length = 255)
    private String nomSignataire;

    @Column
    private LocalDateTime signedAt;

    // Relation avec Chambre (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id", nullable = false)
    @JsonIgnore
    private Chambre chambre;

    // Relation avec User (ManyToOne) - optionnel si non connecte
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private Users users;

    // Metadonnees
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
    }

    /**
     * Etats du dossier de reservation.
     *
     * Transitions valides (niveau 2) :
     *   EN_ATTENTE -> SIGNATURE_EN_COURS -> SIGNATURE_APPOSEE -> CONFIRMEE
     *   EN_ATTENTE -> ANNULEE
     *   SIGNATURE_EN_COURS -> ANNULEE
     *   CONFIRMEE -> TERMINEE
     *   CONFIRMEE -> ANNULEE
     *
     * Au niveau 3 (mock parapheur distant), SIGNATURE_EN_COURS correspond
     * au document envoye au parapheur et SIGNATURE_APPOSEE au webhook de
     * retour confirme par le parapheur.
     */
    public enum StatutReservation {
        EN_ATTENTE,           // Cree, en attente de validation admin
        SIGNATURE_EN_COURS,   // Page de signature ouverte par l'admin
        SIGNATURE_APPOSEE,    // Signature apposee, PDF genere
        CONFIRMEE,            // Dossier valide et confirme au client
        ANNULEE,              // Annule (a tout moment avant TERMINEE)
        TERMINEE              // Sejour termine
    }
}