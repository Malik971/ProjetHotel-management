package com.example.springhotel.integration.pastell.repository;

import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data pour l'entite {@link PastellSync}.
 *
 * Les methodes ci-dessous anticipent les besoins des Lots 4 a 6 :
 *   - Lot 4 (sync montante) : findByReservationId, existsByReservationId, findAllBySyncStatus
 *   - Lot 5 (sync descendante) : findByPastellDocumentId
 *   - Lot 6 (observabilite)   : countBySyncStatus
 *
 * Aucune methode ne modifie l'entite Reservation : PastellSync est une
 * "pellicule" au-dessus de la reservation, jamais une source de verite metier.
 */
@Repository
public interface PastellSyncRepository extends JpaRepository<PastellSync, Long> {

    /**
     * Recupere le suivi Pastell d'une reservation donnee.
     * Utilise a chaque transition metier pour savoir si un dossier Pastell existe deja.
     */
    Optional<PastellSync> findByReservationId(Long reservationId);

    /**
     * Verifie rapidement si une reservation a deja un dossier Pastell.
     * Utilise au Lot 4 pour garantir l'idempotence avant d'appeler create-document.php.
     */
    boolean existsByReservationId(Long reservationId);

    /**
     * Recupere le suivi a partir de l'id_d Pastell.
     * Utilise au Lot 5 quand on recoit un evenement du journal Pastell
     * et qu'on doit retrouver la reservation Sejour concernee.
     */
    Optional<PastellSync> findByPastellDocumentId(String pastellDocumentId);

    /**
     * Tous les syncs dans un statut donne.
     * Utilise au Lot 4 par le job de reprise qui retraite les EN_RETRY et EN_ERREUR.
     */
    List<PastellSync> findAllBySyncStatus(SyncStatus syncStatus);

    /**
     * Page de syncs filtres par statut, tries de la synchro la plus recente
     * a la plus ancienne.
     *
     * Retourne une {@link Page} (au lieu d'une simple {@link List}) pour exposer
     * les metadonnees de pagination cote API admin : nombre total d'elements,
     * nombre total de pages, position courante. Spring Data execute en interne
     * un SELECT COUNT supplementaire dans la meme transaction, ce qui evite
     * un aller-retour reseau cote front.
     *
     * @param status   statut a filtrer
     * @param pageable taille de page et numero de page demandes
     * @return page de syncs (jamais null, peut etre vide)
     */
    Page<PastellSync> findBySyncStatusOrderByDerniereSynchroDesc(
            SyncStatus status, Pageable pageable);

    /**
     * Compte les syncs dans un statut donne (Lot 6).
     * Utilise par l'endpoint d'observabilite {@code GET /api/admin/pastell/status}
     * pour afficher dans le dashboard les compteurs OK / EN_RETRY / DIVERGENCE / EN_ERREUR.
     *
     * Plus efficace que findAllBySyncStatus(status).size() : execute un SELECT COUNT
     * cote base, ne charge aucune entite en memoire.
     */
    long countBySyncStatus(SyncStatus syncStatus);

    /**
     * Recupere les syncs candidats au retraitement par le scheduler (Lot 4 niveau 2).
     *
     * Selectionne les syncs dans les statuts donnes (typiquement EN_RETRY et PENDING),
     * tries du plus ancien au plus recent (FIFO sur date_creation), avec une limite
     * imposee via Pageable pour ne pas noyer Pastell d'un coup apres une grosse panne.
     *
     * @param statuses statuts a inclure (typiquement {EN_RETRY, PENDING})
     * @param pageable typiquement PageRequest.of(0, schedulerBatchSize)
     */
    @Query("""
            SELECT s FROM PastellSync s
            WHERE s.syncStatus IN :statuses
            ORDER BY s.dateCreation ASC
            """)
    List<PastellSync> findCandidatsRetraitement(
            @Param("statuses") Collection<SyncStatus> statuses,
            Pageable pageable);
}