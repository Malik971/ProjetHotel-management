package com.example.springhotel.service;

import com.example.springhotel.dto.DisponibiliteDTO;
import com.example.springhotel.entity.Chambre;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.exception.ResourceNotFoundException;
import com.example.springhotel.repository.ChambreRepository;
import com.example.springhotel.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibiliteService {

    private final ChambreRepository chambreRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Vérifier si une chambre est disponible (retourne boolean)
     */
    public boolean isChambreDisponible(Long chambreId, LocalDate dateDebut, LocalDate dateFin) {
        // Vérifier que la chambre existe
        if (!chambreRepository.existsById(chambreId)) {
            throw new ResourceNotFoundException("Chambre non trouvée avec l'ID : " + chambreId);
        }

        // Vérifier s'il n'y a pas de réservations qui chevauchent
        return !reservationRepository.existsReservationChevauchante(
                chambreId,
                dateDebut,
                dateFin
        );
    }

    /**
     * Vérifier la disponibilité avec détails (retourne DTO)
     */
    public DisponibiliteDTO verifierDisponibilite(Long chambreId, LocalDate dateDebut, LocalDate dateFin) {
        // Vérifier que la chambre existe
        Chambre chambre = chambreRepository.findById(chambreId)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre non trouvée avec l'ID : " + chambreId));

        // Vérifier s'il y a des réservations qui chevauchent
        boolean estDisponible = !reservationRepository.existsReservationChevauchante(
                chambreId,
                dateDebut,
                dateFin
        );

        DisponibiliteDTO response = DisponibiliteDTO.builder()
                .chambreId(chambreId)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .hotelId(chambre.getHotel().getId())
                .disponible(estDisponible)
                .build();

        if (estDisponible) {
            response.setMessage("La chambre est disponible pour les dates sélectionnées");
        } else {
            // Trouver la prochaine disponibilité
            LocalDate prochaineDate = trouverProchaineDisponibilite(chambreId, dateFin);
            response.setProchaineDisponibilite(prochaineDate);
            response.setMessage("La chambre n'est pas disponible. Prochaine disponibilité : " +
                    (prochaineDate != null ? prochaineDate.toString() : "Non disponible"));
        }

        return response;
    }

    /**
     * Trouver la prochaine date de disponibilité
     */
    private LocalDate trouverProchaineDisponibilite(Long chambreId, LocalDate apresDate) {
        // Récupérer toutes les réservations futures de cette chambre
        List<Reservation> reservationsFutures = reservationRepository.findByChambreIdAfterDate(
                chambreId,
                apresDate
        );

        if (reservationsFutures.isEmpty()) {
            return apresDate.plusDays(1);
        }

        // Trouver le premier trou dans les réservations
        LocalDate dateRecherche = apresDate.plusDays(1);
        for (Reservation reservation : reservationsFutures) {
            if (dateRecherche.isBefore(reservation.getDateDebut())) {
                return dateRecherche;
            }
            dateRecherche = reservation.getDateFin().plusDays(1);
        }

        return dateRecherche;
    }

    /**
     * Récupérer toutes les réservations d'une chambre pour une période
     */
    public List<Reservation> getReservationsPourPeriode(Long chambreId, LocalDate dateDebut, LocalDate dateFin) {
        return reservationRepository.findByChambreIdAndDatesBetween(chambreId, dateDebut, dateFin);
    }
}