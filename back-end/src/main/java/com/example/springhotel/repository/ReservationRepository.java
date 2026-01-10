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

    /**
     * Récupérer toutes les réservations d'un utilisateur, triées par date de début décroissante
     */
    List<Reservation> findByUserIdOrderByDateDebutDesc(Long userId);

    /**
     * Vérifier la disponibilité d'une chambre pour une période donnée
     * Retourne les réservations qui chevauchent la période demandée
     */
    @Query("SELECT r FROM Reservation r WHERE r.chambre.id = :chambreId " +
            "AND r.statut != 'ANNULEE' " +
            "AND ((r.dateDebut <= :dateFin AND r.dateFin >= :dateDebut))")
    List<Reservation> findByChambreIdAndDateRange(
            @Param("chambreId") Long chambreId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Vérifier s'il existe une réservation chevauchante (retourne true/false)
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM Reservation r WHERE r.chambre.id = :chambreId " +
            "AND r.statut != 'ANNULEE' " +
            "AND ((r.dateDebut <= :dateFin AND r.dateFin >= :dateDebut))")
    boolean existsReservationChevauchante(
            @Param("chambreId") Long chambreId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Récupérer les réservations d'une chambre après une date donnée
     */
    @Query("SELECT r FROM Reservation r WHERE r.chambre.id = :chambreId " +
            "AND r.statut != 'ANNULEE' " +
            "AND r.dateDebut >= :date " +
            "ORDER BY r.dateDebut ASC")
    List<Reservation> findByChambreIdAfterDate(
            @Param("chambreId") Long chambreId,
            @Param("date") LocalDate date
    );

    /**
     * Récupérer les réservations d'une chambre entre deux dates
     */
    @Query("SELECT r FROM Reservation r WHERE r.chambre.id = :chambreId " +
            "AND r.statut != 'ANNULEE' " +
            "AND ((r.dateDebut BETWEEN :dateDebut AND :dateFin) " +
            "OR (r.dateFin BETWEEN :dateDebut AND :dateFin) " +
            "OR (r.dateDebut <= :dateDebut AND r.dateFin >= :dateFin)) " +
            "ORDER BY r.dateDebut ASC")
    List<Reservation> findByChambreIdAndDatesBetween(
            @Param("chambreId") Long chambreId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    /**
     * Récupérer toutes les réservations d'une chambre
     */
    List<Reservation> findByChambreId(Long chambreId);

    /**
     * Récupérer toutes les réservations d'un hôtel
     */
    @Query("SELECT r FROM Reservation r WHERE r.chambre.hotel.id = :hotelId")
    List<Reservation> findByHotelId(@Param("hotelId") Long hotelId);

    /**
     * Récupérer les réservations par statut
     */
    List<Reservation> findByStatut(Reservation.StatutReservation statut);

    /**
     * Récupérer les réservations par code de confirmation
     */
    @Query("SELECT r FROM Reservation r WHERE r.codeConfirmation = :code")
    Reservation findByCodeConfirmation(@Param("code") String code);

    /**
     * Récupérer les réservations futures d'un utilisateur
     */
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId " +
            "AND r.dateDebut >= :today " +
            "AND r.statut != 'ANNULEE' " +
            "ORDER BY r.dateDebut ASC")
    List<Reservation> findUpcomingReservationsByUserId(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    /**
     * Récupérer les réservations passées d'un utilisateur
     */
    @Query("SELECT r FROM Reservation r WHERE r.user.id = :userId " +
            "AND r.dateFin < :today " +
            "ORDER BY r.dateFin DESC")
    List<Reservation> findPastReservationsByUserId(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );
}