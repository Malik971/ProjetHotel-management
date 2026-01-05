package com.example.springhotel.repository;

import com.example.springhotel.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Récupérer toutes les réservations d'une chambre
    List<Reservation> findByChambreId(Long chambreId);

    // Récupérer les réservations d'un client
    List<Reservation> findByEmailClient(String emailClient);

    // Vérifier si une chambre est disponible entre deux dates
    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r 
        WHERE r.chambre.id = :chambreId 
        AND r.statut != 'ANNULEE' 
        AND (
            (r.dateDebut <= :dateFin AND r.dateFin >= :dateDebut)
        )
    """)
    boolean existsReservationChevauchante(
            @Param("chambreId") Long chambreId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );
}