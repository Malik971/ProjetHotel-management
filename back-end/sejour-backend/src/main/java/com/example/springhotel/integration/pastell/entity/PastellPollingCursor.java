package com.example.springhotel.integration.pastell.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entite mono-ligne qui memorise l'avancement du polling descendant Pastell.
 *<p>
 * Role :
 *   Pour chaque tick du {@link com.example.springhotel.integration.pastell.scheduler.PastellPollingScheduler},
 *   on lit ce curseur (lastProcessedIdJ), on demande au mock Pastell tout ce qui
 *   est plus recent que cette valeur, on traite chaque entree, et on remet a jour
 *   le curseur. Une seule ligne globale au processus suffit.
 *<p>
 * Pourquoi une entite plutot qu'une lecture SQL brute ?
 *   - Coherence avec le reste du code Pastell qui utilise JPA.
 *   - Hibernate gere automatiquement les horodatages via @PreUpdate.
 *   - Pour les tests sous H2 + Hibernate ddl-auto=create-drop, l'entite est
 *     creee automatiquement avec le bon schema (la migration Flyway V4 n'est
 *     pas executee en test).
 *<p>
 * Pourquoi pas un singleton applicatif (champ static volatile) ?
 *   - On veut que le curseur SURVIVE aux redemarrages de l'application.
 *     Sans persistance, on rejouerait tout le journal a chaque restart,
 *     ce qui est couteux et casserait l'idempotence sur les transitions
 *     non commutatives (ex : on remettrait une reservation TERMINEE).
 *   - On veut aussi que deux instances de l'application (cas d'un scaling
 *     horizontal) lisent et mettent a jour le meme curseur. Stocker en base
 *     resout ce probleme tout seul.
 *<p>
 * Concurrence et ID fixe a 1 :
 *   La PK est forcee a 1 par contrainte CHECK SQL (V4). Le code n'utilise
 *   QUE l'id 1 pour acceder au curseur. Si un jour le scheduler tourne sur
 *   plusieurs instances, on ajoutera un verrou (SELECT ... FOR UPDATE) pour
 *   serialiser les acces. Pour le Lot 5, on accepte le risque sur deux instances
 *   simultanees : la pire chose qui arrive, c'est qu'on traite 2 fois la meme
 *   entree, ce qui est idempotent grace au mapping action -> statut.
 */
@Entity
@Table(name = "pastell_polling_cursor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastellPollingCursor {

    /**
     * Identifiant fige a 1 par la contrainte CHECK en base.
     * Ne pas utiliser de @GeneratedValue : on veut explicitement la valeur 1.
     */
    @Id
    @Column(name = "id")
    private Long id;

    /**
     * Dernier id_j du journal Pastell traite avec succes.
     * Au premier demarrage, vaut 0 (initialise par la migration V4).
     * Cette valeur n'est PAS un compteur local : c'est strictement un miroir
     * de l'id_j cote Pastell, qui est une sequence monotone cote mock
     * (AtomicLong dans MockDocumentStore).
     */
    @Column(name = "last_processed_id_j", nullable = false)
    @Builder.Default
    private Long lastProcessedIdJ = 0L;

    /**
     * Horodatage du dernier polling reussi.
     * Null tant qu'aucun polling n'a tourne. Utile pour le dashboard Lot 6
     * ("dernier contact avec Pastell il y a X minutes").
     */
    @Column(name = "last_polled_at")
    private LocalDateTime lastPolledAt;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;

    /**
     * Callback JPA invoque avant l'INSERT.
     *<p>
     * Pour le runtime prod, cette methode ne sera JAMAIS appelee : la ligne
     * unique est creee par la migration Flyway V4 a l'install. Mais pour les
     * tests sous H2 + Hibernate ddl-auto=create-drop, Flyway est desactive et
     * on doit pouvoir creer la ligne au runtime, donc on prepare le terrain ici.
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.dateCreation == null) {
            this.dateCreation = now;
        }
        this.dateModification = now;
        if (this.lastProcessedIdJ == null) {
            this.lastProcessedIdJ = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}