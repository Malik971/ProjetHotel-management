package com.example.springhotel.integration.pastell.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entite de synchronisation entre une Reservation et son dossier Pastell.
 *
 * Choix de design :
 *   - Pas de relation JPA vers Reservation (pas de @ManyToOne, pas de @OneToOne).
 *     Juste un {@code reservation_id} en Long. Motivation : decoupler
 *     la couche integration Pastell du domaine metier. Si demain le package
 *     {@code com.example.springhotel.integration.pastell} doit etre extrait
 *     en bibliotheque reutilisable, aucune dependance Reservation a retirer.
 *   - Pour recuperer la Reservation associee, passer par ReservationRepository
 *     avec l'id ici stocke.
 *
 * Invariants :
 *   - reservationId est UNIQUE : une reservation = un seul dossier Pastell.
 *   - pastellDocumentId est UNIQUE : pas de partage entre reservations.
 *   - syncStatus n'est jamais null.
 */
@Entity
@Table(name = "pastell_sync")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastellSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant de la reservation Sejour associee.
     * Pas de relation JPA : decouplage volontaire (voir javadoc de la classe).
     */
    @Column(name = "reservation_id", nullable = false, unique = true)
    private Long reservationId;

    /**
     * id_d retourne par Pastell lors du create-document.php.
     * Stocke des le premier succes pour garantir l'idempotence :
     * on ne recree jamais un dossier deja cree.
     */
    @Column(name = "pastell_document_id", nullable = false, unique = true, length = 100)
    private String pastellDocumentId;

    /**
     * Dernier etat connu cote Pastell (observe via polling journal.php au Lot 5).
     * Peut etre null juste apres la creation, avant le premier poll.
     */
    @Column(name = "pastell_etat_dernier_connu", length = 100)
    private String pastellEtatDernierConnu;

    /**
     * Statut technique de la synchronisation.
     * Voir {@link SyncStatus} pour la semantique de chaque valeur.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    private SyncStatus syncStatus;

    /**
     * Compteur de tentatives pour le retry.
     * Utilise au Lot 4 par le mecanisme de retry exponentiel.
     */
    @Column(name = "tentatives", nullable = false)
    @Builder.Default
    private Integer tentatives = 0;

    /**
     * Message d'erreur de la derniere tentative echouee.
     * Tronque a 1000 caracteres au niveau applicatif pour eviter d'exploser la base.
     */
    @Column(name = "derniere_erreur", columnDefinition = "TEXT")
    private String derniereErreur;

    /**
     * Horodatage de la derniere tentative de synchronisation (reussie ou non).
     */
    @Column(name = "derniere_synchro")
    private LocalDateTime derniereSynchro;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.dateCreation = now;
        this.dateModification = now;
        if (this.syncStatus == null) {
            this.syncStatus = SyncStatus.EN_RETRY;
        }
        if (this.tentatives == null) {
            this.tentatives = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}