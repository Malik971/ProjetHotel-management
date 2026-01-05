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

    // Récupérer toutes les chambres d'un hôtel
    List<Chambre> findByHotelId(Long hotelId);

    // Récupérer les chambres disponibles entre deux dates
    @Query("""
        SELECT c FROM Chambre c 
        WHERE c.id NOT IN (
            SELECT r.chambre.id FROM Reservation r 
            WHERE r.statut != 'ANNULEE' 
            AND (
                (r.dateDebut <= :dateFin AND r.dateFin >= :dateDebut)
            )
        )
    """)
    List<Chambre> findChambresDisponibles(
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    // Récupérer les chambres disponibles d'un hôtel spécifique
    @Query("""
        SELECT c FROM Chambre c 
        WHERE c.hotel.id = :hotelId 
        AND c.id NOT IN (
            SELECT r.chambre.id FROM Reservation r 
            WHERE r.statut != 'ANNULEE' 
            AND (
                (r.dateDebut <= :dateFin AND r.dateFin >= :dateDebut)
            )
        )
    """)
    List<Chambre> findChambresDisponiblesParHotel(
            @Param("hotelId") Long hotelId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );
}