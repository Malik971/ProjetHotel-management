package com.example.springhotel.integration.pastell.repository;

import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data pour l'entite {@link PastellSync}.
 *
 * Les methodes ci-dessous anticipent les besoins des Lots 4 et 5 :
 *   - Lot 4 (sync montante) : findByReservationId, existsByReservationId, findAllBySyncStatus
 *   - Lot 5 (sync descendante) : findByPastellDocumentId
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
     * Recupere les syncs candidats au retraitement par le scheduler (Lot 4 niveau 2).
     *
     * Selectionne les syncs dans les statuts donnes (typiquement EN_RETRY et PENDING),
     * tries du plus ancien au plus recent (FIFO sur date_creation), avec une limite
     * imposee via Pageable pour ne pas noyer Pastell d'un coup apres une grosse panne.
     *
     * Pourquoi un FIFO ?
     *   - Equite : les syncs qui attendent depuis le plus longtemps passent en
     *     premier, ils ne sont jamais affames par les nouveaux arrivants.
     *   - Predictibilite : sous charge, l'admin sait que c'est toujours les plus
     *     vieux qui vont passer.
     *
     * Pourquoi PENDING aussi ?
     *   - Un PENDING qui traine sans avoir bascule OK ou EN_RETRY est forcement
     *     orphelin (le serveur a crashe entre la persistance et l'appel HTTP).
     *   - On le retraite comme un EN_RETRY : le scheduler appellera retraiterSync,
     *     qui executera l'appel HTTP que le crash avait empeche.
     *
     * Pourquoi @Query JPQL plutot que le naming method de Spring Data ?
     *   - Le nom genere "findTopNBySyncStatusInOrderByDateCreationAsc" est
     *     correct mais long. Une @Query est plus lisible et plus explicite
     *     dans son intention.
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