package com.example.springhotel.integration.pastell.repository;

import com.example.springhotel.integration.pastell.entity.PastellJournalEntryRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la trace persistee du journal Pastell.
 *<p>
 * Utilise par :
 *   - {@code PastellJournalEntryProcessor} : pour persister chaque entree traitee
 *   - {@code AdminPastellController} : pour recuperer le journal d'un dossier
 *     et le flux d'activite recente
 */
@Repository
public interface PastellJournalEntryRecordRepository
        extends JpaRepository<PastellJournalEntryRecord, Long> {

    /**
     * Toutes les entrees pour un dossier Pastell, ordonnees chronologiquement.
     * Utilise par la page detail admin pour afficher la frise.
     */
    List<PastellJournalEntryRecord> findByPastellDocumentIdOrderByOccurredAtAsc(
            String pastellDocumentId
    );

    /**
     * Existe deja une entree avec ce idJ ? Permet d'eviter les doublons
     * si le polling repasse sur les memes entrees.
     */
    Optional<PastellJournalEntryRecord> findByIdJ(Long idJ);

    boolean existsByIdJ(Long idJ);

    /**
     * Les N entrees les plus recentes, tous dossiers confondus.
     * Utilise pour le flux d'activite du dashboard admin.
     */
    @Query("SELECT j FROM PastellJournalEntryRecord j ORDER BY j.occurredAt DESC")
    List<PastellJournalEntryRecord> findAllRecent(Pageable pageable);
}