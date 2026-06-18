package com.example.springhotel.controller;

import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.dto.SignatureRequestDTO;
import com.example.springhotel.dto.SignatureResponseDTO;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.repository.ReservationRepository;
import com.example.springhotel.service.ReservationService;
import com.example.springhotel.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Controller admin pour le workflow de signature electronique.
 *
 * Endpoints :
 *
 *   GET  /api/admin/reservations/en-attente
 *     Liste des reservations a traiter (statut EN_ATTENTE ou SIGNATURE_EN_COURS).
 *     Utilise par la page AdminReservationsEnAttente.jsx.
 *
 *   POST /api/admin/reservations/{id}/initier-signature
 *     Passe la reservation de EN_ATTENTE a SIGNATURE_EN_COURS.
 *     Appele quand l'admin ouvre la page de signature.
 *
 *   POST /api/admin/reservations/{id}/signer
 *     Corps : { signatureBase64, nomSignataire }
 *     Genere le PDF, applique la signature, passe le dossier a CONFIRMEE.
 *
 *   GET  /api/admin/reservations/{id}/pdf
 *     Retourne le PDF signe en base64 pour telechargement ou affichage.
 *
 * Securite : herite de SecurityConfig - /api/admin/** exige ADMIN ou EMPLOYE.
 */
@RestController
@RequestMapping("/api/admin/reservations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminSignatureController {

    private static final Logger log = LoggerFactory.getLogger(AdminSignatureController.class);

    private final SignatureService signatureService;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    /**
     * Liste des reservations en attente de validation.
     *
     * Retourne EN_ATTENTE et SIGNATURE_EN_COURS, triees par date de creation
     * decroissante, paginees. Le frontend affiche les plus recentes en premier.
     */
    @GetMapping("/en-attente")
    public ResponseEntity<List<ReservationResponseDTO>> getEnAttente(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "dateCreation"));

        Page<Reservation> reservations = reservationRepository
                .findByStatutIn(
                        List.of(
                                Reservation.StatutReservation.EN_ATTENTE,
                                Reservation.StatutReservation.SIGNATURE_EN_COURS
                        ),
                        pageRequest
                );

        List<ReservationResponseDTO> dtos = reservations.getContent()
                .stream()
                .map(reservationService::convertToDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Initie la signature : passe la reservation a SIGNATURE_EN_COURS.
     *
     * Appele automatiquement par le frontend quand l'admin ouvre la page
     * de signature. Permet de savoir qu'un agent a pris le dossier en charge,
     * meme si la signature n'est pas encore apposee.
     */
    @PostMapping("/{id}/initier-signature")
    public ResponseEntity<?> initierSignature(@PathVariable Long id) {
        try {
            signatureService.initierSignature(id);
            return ResponseEntity.ok(Map.of(
                    "reservationId", id,
                    "statut", Reservation.StatutReservation.SIGNATURE_EN_COURS.name()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Apposer la signature sur le dossier.
     *
     * Corps attendu : { "signatureBase64": "...", "nomSignataire": "..." }
     *
     * Le service genere le PDF, persiste la signature, et fait passer le
     * dossier a SIGNATURE_APPOSEE puis CONFIRMEE en auto-transition.
     * Deux StatutChangeEvent sont publies (un par transition), ce qui
     * declenche les emails correspondants.
     */
    @PostMapping("/{id}/signer")
    public ResponseEntity<?> signer(
            @PathVariable Long id,
            @RequestBody SignatureRequestDTO request) {

        if (request.signatureBase64() == null || request.signatureBase64().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "signatureBase64 est requis"));
        }
        if (request.nomSignataire() == null || request.nomSignataire().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "nomSignataire est requis"));
        }

        try {
            SignatureResponseDTO response = signatureService.signerReservation(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Telecharger le PDF du recepisse signe.
     *
     * Retourne le base64 du PDF pour que le frontend puisse l'ouvrir
     * dans un nouvel onglet ou le telecharger via un lien data:.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> getPdf(@PathVariable Long id) {
        return reservationRepository.findById(id)
                .map(reservation -> {
                    if (reservation.getSignaturePdfBase64() == null) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(Map.of(
                            "reservationId", id,
                            "pdfBase64", reservation.getSignaturePdfBase64(),
                            "nomSignataire", reservation.getNomSignataire() != null
                                    ? reservation.getNomSignataire()
                                    : "",
                            "signedAt", reservation.getSignedAt() != null
                                    ? reservation.getSignedAt().toString()
                                    : ""
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}