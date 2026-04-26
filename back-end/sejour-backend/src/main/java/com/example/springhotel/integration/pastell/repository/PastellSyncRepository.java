package com.example.springhotel.integration.pastell.repository;

import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}