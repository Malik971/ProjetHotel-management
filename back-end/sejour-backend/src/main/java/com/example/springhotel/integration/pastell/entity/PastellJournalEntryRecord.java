package com.example.springhotel.integration.pastell.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Trace persistee d'une entree du journal Pastell consommee par Sejour.
 *<p>
 * <b>Pourquoi cette entite existe :</b>
 *<p>
 * Le DTO {@link com.example.springhotel.integration.pastell.client.PastellJournalEntry}
 * represente une entree TELLE QUE RECUE de l'API Pastell. C'est un objet
 * de transport, jete une fois traite par le {@code PastellJournalEntryProcessor}.
 *<p>
 * Cette entite, en revanche, est une SAUVEGARDE locale persistee en base.
 * Elle permet a Sejour de reconstituer la frise des evenements d'un dossier
 * sans avoir a re-interroger Pastell a chaque consultation admin.
 *<p>
 * <b>Pourquoi "Record" dans le nom ?</b>
 *<p>
 * Pour distinguer clairement de {@code PastellJournalEntry} (le DTO Jackson).
 * "Record" au sens "trace persistee dans la base", pas au sens des records Java.
 *<p>
 * <b>Strategie de remplissage :</b>
 *<p>
 * A chaque entree traitee par {@code PastellJournalEntryProcessor.processEntry()},
 * on persiste un PastellJournalEntryRecord. C'est le service qui orchestre,
 * pas l'entite qui se sauve elle-meme : cette entite est un POJO JPA simple.
 *<p>
 * <b>Index utiles :</b>
 *<p>
 *   - {@code id_d_pastell} : index pour retrouver tous les evenements d'un dossier
 *   - {@code id_j} : unique pour eviter les doublons en cas de re-poll
 *   - {@code occurred_at} : index pour le tri chronologique du flux d'activite
 */
@Entity
@Table(
        name = "pastell_journal_entry_record",
        indexes = {
                @Index(name = "idx_journal_doc_id", columnList = "id_d_pastell"),
                @Index(name = "idx_journal_occurred_at", columnList = "occurred_at"),
                @Index(name = "uk_journal_id_j", columnList = "id_j", unique = true)
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PastellJournalEntryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * idJ Pastell : identifiant unique monotone de l'entree journal.
     * Sert aussi de cle d'unicite pour eviter les doublons (un re-poll
     * du meme since_id_j renverrait les memes entrees).
     */
    @Column(name = "id_j", nullable = false, unique = true)
    private Long idJ;

    /**
     * id_d Pastell : identifiant du dossier (document) concerne.
     * Permet de joindre avec PastellSync.pastellDocumentId.
     */
    @Column(name = "id_d_pastell", length = 64, nullable = false)
    private String pastellDocumentId;

    /**
     * Action enregistree dans le journal (ex: "creation", "validee",
     * "confirmee", "terminee", "annulee").
     */
    @Column(name = "action", length = 64, nullable = false)
    private String action;

    /**
     * id_e Pastell : entite Pastell concernee. Sejour est mono-entite,
     * donc en general 1.
     */
    @Column(name = "id_entite_pastell")
    private Long idEntitePastell;

    /**
     * Horodatage de l'evenement cote Pastell (issu du champ "date" de
     * l'API journal).
     */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /**
     * Date de la sauvegarde cote Sejour. Pas la meme chose que occurredAt :
     * recordedAt peut etre plus tardif si le polling a ete en retard.
     */
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    /**
     * Severite logique : INFO | WARN | ERROR. Calcule par le processor
     * a partir de l'action (ex: "annulee" -> ERROR).
     */
    @Column(name = "severity", length = 16)
    private String severity;

    /**
     * Message libre (description courte de l'evenement, pour affichage UI).
     */
    @Column(name = "message", length = 500)
    private String message;
}