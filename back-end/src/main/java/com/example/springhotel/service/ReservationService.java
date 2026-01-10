package com.example.springhotel.service;

import com.example.springhotel.dto.ReservationRequestDTO;
import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.entity.Chambre;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.entity.User;
import com.example.springhotel.repository.ChambreRepository;
import com.example.springhotel.repository.ReservationRepository;
import com.example.springhotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ChambreRepository chambreRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public ReservationResponseDTO creerReservation(ReservationRequestDTO request, String userEmail) {
        // 1. Récupérer la chambre
        Chambre chambre = chambreRepository.findById(request.getChambreId())
                .orElseThrow(() -> new RuntimeException("Chambre non trouvée"));

        // 2. Récupérer l'utilisateur (optionnel si connecté)
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        // 3. Vérifier la disponibilité
        boolean disponible = verifierDisponibilite(
                request.getChambreId(),
                request.getDateDebut(),
                request.getDateFin()
        );

        if (!disponible) {
            throw new RuntimeException("Chambre non disponible pour ces dates");
        }

        // 4. Calculer le prix total
        long nombreNuits = ChronoUnit.DAYS.between(request.getDateDebut(), request.getDateFin());
        double prixTotal = chambre.getPrixParNuit().doubleValue() * nombreNuits;

        // 5. Générer un code de confirmation
        String codeConfirmation = genererCodeConfirmation();

        // 6. Créer la réservation
        Reservation reservation = Reservation.builder()
                .chambre(chambre)
                .user(user)
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .nomClient(request.getNomClient())
                .emailClient(request.getEmailClient())
                .telephoneClient(request.getTelephoneClient())
                .nombrePersonnes(request.getNombrePersonnes())
                .prixTotal(prixTotal)
                .statut(Reservation.StatutReservation.CONFIRMEE)
                .codeConfirmation(codeConfirmation)
                .build();

        // 7. Sauvegarder
        Reservation saved = reservationRepository.save(reservation);

        // 8. Envoyer l'email de confirmation
        try {
            emailService.envoyerEmailConfirmation(saved);
        } catch (Exception e) {
            System.err.println("⚠️ Erreur envoi email (réservation créée quand même) : " + e.getMessage());
        }

        // 9. Convertir en DTO et retourner
        return convertToDTO(saved);
    }

    private boolean verifierDisponibilite(Long chambreId, LocalDate dateDebut, LocalDate dateFin) {
        return reservationRepository.findByChambreIdAndDateRange(chambreId, dateDebut, dateFin).isEmpty();
    }

    private String genererCodeConfirmation() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ReservationResponseDTO convertToDTO(Reservation reservation) {
        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .userId(reservation.getUser() != null ? reservation.getUser().getId() : null)
                .chambreId(reservation.getChambre().getId())
                .chambreNom(reservation.getChambre().getNom())
                .hotelId(reservation.getChambre().getHotel().getId())
                .hotelNom(reservation.getChambre().getHotel().getNom())
                .hotelVille(reservation.getChambre().getHotel().getVille())
                .hotelImageUrl(reservation.getChambre().getHotel().getImageUrl())
                .dateDebut(reservation.getDateDebut())
                .dateFin(reservation.getDateFin())
                .nomClient(reservation.getNomClient())
                .emailClient(reservation.getEmailClient())
                .telephoneClient(reservation.getTelephoneClient())
                .nombrePersonnes(reservation.getNombrePersonnes())
                .prixTotal(BigDecimal.valueOf(reservation.getPrixTotal()))
                .statut(reservation.getStatut())
                .codeConfirmation(reservation.getCodeConfirmation())
                .build();
    }
}