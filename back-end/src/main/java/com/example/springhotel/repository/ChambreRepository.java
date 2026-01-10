package com.example.springhotel.repository;

import com.example.springhotel.entity.Chambre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChambreRepository extends JpaRepository<Chambre, Long> {

    // Récupère les chambres d'un hôtel
    List<Chambre> findByHotelId(Long hotelId);

    // Récupère les chambres disponibles pour un hôtel et une période
    @Query("SELECT c FROM Chambre c WHERE c.hotel.id = :hotelId " +
            "AND c.id NOT IN (" +
            "  SELECT r.chambre.id FROM Reservation r " +
            "  WHERE r.statut != 'ANNULEE' " +
            "  AND ((r.dateDebut BETWEEN :dateDebut AND :dateFin) " +
            "  OR (r.dateFin BETWEEN :dateDebut AND :dateFin) " +
            "  OR (r.dateDebut <= :dateDebut AND r.dateFin >= :dateFin))" +
            ")")
    List<Chambre> findChambresDisponiblesParHotel(
            @Param("hotelId") Long hotelId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    // Récupère toutes les chambres disponibles pour une période
    @Query("SELECT c FROM Chambre c WHERE c.id NOT IN (" +
            "  SELECT r.chambre.id FROM Reservation r " +
            "  WHERE r.statut != 'ANNULEE' " +
            "  AND ((r.dateDebut BETWEEN :dateDebut AND :dateFin) " +
            "  OR (r.dateFin BETWEEN :dateDebut AND :dateFin) " +
            "  OR (r.dateDebut <= :dateDebut AND r.dateFin >= :dateFin))" +
            ")")
    List<Chambre> findChambresDisponibles(
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );
}