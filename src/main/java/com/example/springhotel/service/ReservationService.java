package com.example.springhotel.service;

import com.example.springhotel.dto.ReservationDTO;
import com.example.springhotel.entity.Chambre;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.exception.ChambreNonDisponibleException;
import com.example.springhotel.exception.ResourceNotFoundException;
import com.example.springhotel.repository.ChambreRepository;
import com.example.springhotel.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ChambreRepository chambreRepository;

    // Mapper : Entity -> DTO
    private ReservationDTO toDTO(Reservation reservation) {
        return ReservationDTO.builder()
                .id(reservation.getId())
                .dateDebut(reservation.getDateDebut())
                .dateFin(reservation.getDateFin())
                .nomClient(reservation.getNomClient())
                .emailClient(reservation.getEmailClient())
                .telephoneClient(reservation.getTelephoneClient())
                .nombrePersonnes(reservation.getNombrePersonnes())
                .prixTotal(reservation.getPrixTotal())
                .statut(reservation.getStatut())
                .chambreId(reservation.getChambre().getId())
                .chambreNom(reservation.getChambre().getNom())
                .build();
    }

    // Mapper : DTO -> Entity
    private Reservation toEntity(ReservationDTO dto) {
        Chambre chambre = chambreRepository.findById(dto.getChambreId())
                .orElseThrow(() -> new ResourceNotFoundException("Chambre non trouvée avec l'ID : " + dto.getChambreId()));

        return Reservation.builder()
                .id(dto.getId())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .nomClient(dto.getNomClient())
                .emailClient(dto.getEmailClient())
                .telephoneClient(dto.getTelephoneClient())
                .nombrePersonnes(dto.getNombrePersonnes())
                .prixTotal(dto.getPrixTotal())
                .statut(dto.getStatut())
                .chambre(chambre)
                .build();
    }

    // Calculer le prix total
    private BigDecimal calculerPrixTotal(Chambre chambre, ReservationDTO dto) {
        long nombreNuits = ChronoUnit.DAYS.between(dto.getDateDebut(), dto.getDateFin());
        return chambre.getPrixParNuit().multiply(BigDecimal.valueOf(nombreNuits));
    }

    // CREATE
    @Transactional
    public ReservationDTO creerReservation(ReservationDTO reservationDTO) {
        // Vérifier la disponibilité
        boolean estDisponible = !reservationRepository.existsReservationChevauchante(
                reservationDTO.getChambreId(),
                reservationDTO.getDateDebut(),
                reservationDTO.getDateFin()
        );

        if (!estDisponible) {
            throw new ChambreNonDisponibleException(
                    "La chambre n'est pas disponible pour les dates sélectionnées : " +
                            reservationDTO.getDateDebut() + " - " + reservationDTO.getDateFin()
            );
        }

        // Récupérer la chambre
        Chambre chambre = chambreRepository.findById(reservationDTO.getChambreId())
                .orElseThrow(() -> new ResourceNotFoundException("Chambre non trouvée avec l'ID : " + reservationDTO.getChambreId()));

        // Calculer le prix total
        BigDecimal prixTotal = calculerPrixTotal(chambre, reservationDTO);
        reservationDTO.setPrixTotal(prixTotal);

        // Créer la réservation
        Reservation reservation = toEntity(reservationDTO);
        Reservation saved = reservationRepository.save(reservation);
        return toDTO(saved);
    }

    // READ ALL
    public List<ReservationDTO> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // READ BY ID
    public ReservationDTO getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'ID : " + id));
        return toDTO(reservation);
    }

    // READ BY CHAMBRE
    public List<ReservationDTO> getReservationsByChambre(Long chambreId) {
        return reservationRepository.findByChambreId(chambreId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // READ BY CLIENT
    public List<ReservationDTO> getReservationsByClient(String email) {
        return reservationRepository.findByEmailClient(email).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // UPDATE STATUT
    @Transactional
    public ReservationDTO updateStatut(Long id, Reservation.StatutReservation nouveauStatut) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'ID : " + id));

        reservation.setStatut(nouveauStatut);
        Reservation updated = reservationRepository.save(reservation);
        return toDTO(updated);
    }

    // DELETE
    @Transactional
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Réservation non trouvée avec l'ID : " + id);
        }
        reservationRepository.deleteById(id);
    }

    // VÉRIFIER DISPONIBILITÉ
    public boolean verifierDisponibilite(Long chambreId, ReservationDTO reservationDTO) {
        return !reservationRepository.existsReservationChevauchante(
                chambreId,
                reservationDTO.getDateDebut(),
                reservationDTO.getDateFin()
        );
    }
}