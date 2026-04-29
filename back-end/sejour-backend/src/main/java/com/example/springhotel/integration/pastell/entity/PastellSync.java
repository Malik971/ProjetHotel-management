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
 *   - pastellDocumentId est UNIQUE quand non null : pas de partage entre reservations.
 *     Peut etre null tant que le premier appel create-document.php n'a pas reussi
 *     (statut PENDING ou EN_RETRY avant premier succes).
 *   - syncStatus n'est jamais null (garanti par @PrePersist).
 *
 * Note sur le ON DELETE CASCADE :
 *   La cascade est definie cote SQL (V2__pastell_sync_table.sql), pas en JPA.
 *   Si une reservation est supprimee en base, son PastellSync disparait aussi.
 *   Ce choix evite de coupler l'entite JPA a Reservation tout en gardant
 *   l'integrite referentielle en base.
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
     *
     * Nullable : un PastellSync en statut PENDING ou EN_RETRY (avant premier succes)
     * n'a pas encore recu d'id_d. La colonne est remplie des le premier appel reussi
     * a create-document.php et ne change plus ensuite.
     *
     * La contrainte UNIQUE en base accepte plusieurs NULL (norme SQL standard),
     * ce qui permet d'avoir plusieurs syncs en PENDING sans conflit.
     *
     * L'idempotence est garantie par la contrainte UNIQUE sur reservation_id :
     * on ne cree jamais deux PastellSync pour la meme reservation.
     */
    @Column(name = "pastell_document_id", unique = true, length = 100)
    private String pastellDocumentId;

    /**
     * Dernier etat connu cote Pastell (observe via polling journal.php au Lot 5).
     * Peut etre null juste apres la creation, avant le premier poll.
     */
    @Column(name = "pastell_etat_dernier_connu", length = 100)
    private String pastellEtatDernierConnu;

    /**
     * Statut technique de la synchronisation.
     * Voir {@link SyncStatus} pour la semantique de chaque valeur et le cycle de vie.
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

    /**
     * Callback JPA avant la premiere insertion en base.
     *
     * Initialise les horodatages et les valeurs par defaut.
     * Le statut par defaut est PENDING (et non EN_RETRY) car a ce stade
     * aucun appel HTTP n'a encore ete tente. PENDING signifie "intention
     * de synchroniser enregistree, appel pas encore lance".
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.dateCreation = now;
        this.dateModification = now;
        if (this.syncStatus == null) {
            this.syncStatus = SyncStatus.PENDING;
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