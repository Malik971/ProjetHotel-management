package com.example.springhotel.service;

import com.example.springhotel.dto.ReservationRequestDTO;
import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.entity.Chambre;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.ChambreRepository;
import com.example.springhotel.repository.ReservationRepository;
import com.example.springhotel.repository.UserRepository;
import com.example.springhotel.reservation.event.ReservationCreatedEvent;
import com.example.springhotel.reservation.event.StatutChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Cree une nouvelle reservation et la place en statut EN_ATTENTE.
     *
     * Pourquoi EN_ATTENTE et non CONFIRMEE directement ?
     *   - Avant ce lot, le statut passait a CONFIRMEE a la creation, ce qui
     *     court-circuitait la validation admin et rendait Pastell decoratif.
     *   - Desormais, le circuit est : creation (EN_ATTENTE) -> validation admin
     *     avec signature (SIGNATURE_EN_COURS -> SIGNATURE_APPOSEE) -> CONFIRMEE.
     *   - Pastell orchestre cette validation : il recoit le dossier des la creation
     *     et notifie Spring a chaque changement d'etape circuit, rendant l'integration
     *     reelle et observable.
     */
    @Transactional
    public ReservationResponseDTO creerReservation(ReservationRequestDTO request, String userEmail) {

        // 1. Recuperer la chambre
        Chambre chambre = chambreRepository.findById(request.getChambreId())
                .orElseThrow(() -> new RuntimeException("Chambre non trouvee"));

        // 2. Recuperer l'utilisateur (optionnel)
        Users users = null;
        if (userEmail != null) {
            users = userRepository.findByEmail(userEmail).orElse(null);
        }

        // 3. Verifier la disponibilite
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

        // 5. Generer un code de confirmation
        String codeConfirmation = genererCodeConfirmation();

        // 6. Creer la reservation en EN_ATTENTE (pas CONFIRMEE : voir javadoc classe)
        Reservation reservation = Reservation.builder()
                .chambre(chambre)
                .users(users)
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .nomClient(request.getNomClient())
                .emailClient(request.getEmailClient())
                .telephoneClient(request.getTelephoneClient())
                .nombrePersonnes(request.getNombrePersonnes())
                .prixTotal(prixTotal)
                .statut(Reservation.StatutReservation.EN_ATTENTE)
                .codeConfirmation(codeConfirmation)
                .build();

        // 7. Sauvegarder
        Reservation saved = reservationRepository.save(reservation);

        // 8. Publier l'evenement de creation (pour Pastell, APRES commit).
        //    TransactionalEventListener AFTER_COMMIT cote listener : si la transaction
        //    echoue, le listener n'est jamais invoque.
        eventPublisher.publishEvent(new ReservationCreatedEvent(saved.getId()));

        // 9. Notifier le client : "votre demande est en cours de traitement".
        //    Decouple via evenement : le listener envoie l'email hors transaction.
        eventPublisher.publishEvent(new StatutChangeEvent(
                saved.getId(),
                null,
                Reservation.StatutReservation.EN_ATTENTE
        ));

        // 10. Retourner le DTO
        return convertToDTO(saved);
    }

    private boolean verifierDisponibilite(Long chambreId, LocalDate dateDebut, LocalDate dateFin) {
        return reservationRepository.findByChambreIdAndDateRange(chambreId, dateDebut, dateFin).isEmpty();
    }

    private String genererCodeConfirmation() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public ReservationResponseDTO convertToDTO(Reservation reservation) {
        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .userId(reservation.getUsers() != null ? reservation.getUsers().getId() : null)
                .chambreId(reservation.getChambre().getId())
                .chambreNom(reservation.getChambre().getNom())
                .hotelId(reservation.getChambre().getHotel().getId())
                .hotelNom(reservation.getChambre().getHotel().getNom())
                .hotelVille(reservation.getChambre().getHotel().getVille())
                .hotelImageUrl(
                        reservation.getChambre().getHotel().getImageUrls().stream()
                                .findFirst()
                                .orElse(null)
                )
                .dateDebut(reservation.getDateDebut())
                .dateFin(reservation.getDateFin())
                .nomClient(reservation.getNomClient())
                .emailClient(reservation.getEmailClient())
                .telephoneClient(reservation.getTelephoneClient())
                .nombrePersonnes(reservation.getNombrePersonnes())
                .prixTotal(BigDecimal.valueOf(reservation.getPrixTotal()))
                .statut(reservation.getStatut())
                .codeConfirmation(reservation.getCodeConfirmation())
                .nomSignataire(reservation.getNomSignataire())
                .signedAt(reservation.getSignedAt())
                .pdfDisponible(reservation.getSignaturePdfBase64() != null)
                .build();
    }
}