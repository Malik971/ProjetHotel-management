package com.example.springhotel.controller;

import com.example.springhotel.dto.ReservationResponseDTO;
import com.example.springhotel.dto.ReservationTimelineDTO;
import com.example.springhotel.dto.ReservationTimelineDTO.SuiviAdministratif;
import com.example.springhotel.dto.ReservationTimelineDTO.TimelineEtapeDTO;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.entity.Users;
import com.example.springhotel.integration.pastell.entity.PastellSync;
import com.example.springhotel.integration.pastell.entity.SyncStatus;
import com.example.springhotel.integration.pastell.repository.PastellSyncRepository;
import com.example.springhotel.repository.ReservationRepository;
import com.example.springhotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Endpoints client pour la gestion des reservations.
 *
 * Lot 2 option A2 : la timeline retournee par /timeline contient maintenant
 * DEUX volets distincts :
 *   un, etapesSejour : 4 etapes orientees experience voyageur, calculees
 *       a partir du statut metier et des dates de sejour,
 *   deux, suiviAdministratif : bloc decrivant l'etat du dossier dans
 *       le parapheur Pastell, en libelle client adapte.
 *
 * Cette separation reflete la philosophie de Pastell chez les collectivites :
 * l'agent technique a sa vue detaillee (espace admin), l'usager final voit
 * son experience metier avec un acces optionnel aux details administratifs.
 *
 * Principe de securite : on lit toujours l'identite depuis Authentication
 * (donc depuis le JWT), jamais depuis un parametre URL.
 *
 * Lot 3 (sous-lot annulation) : la suppression d'une reservation est
 * conditionnee par une regle metier stricte. Une reservation n'est
 * annulable que si elle est strictement "a venir". Les sejours en cours
 * ou termines sont irrevocables. Le contrat est explicite cote API :
 * un 409 Conflict est renvoye avec un message clair en cas de violation.
 */
@RestController
@RequestMapping("/api/client/reservations")
@RequiredArgsConstructor
public class ClientReservationController {

    /**
     * Nombre de jours avant la date d'arrivee a partir duquel on considere
     * que l'hotel "prepare" l'arrivee du client (etape 2 de la timeline).
     */
    private static final long JOURS_AVANT_PREPARATION = 7L;

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final PastellSyncRepository pastellSyncRepository;

    @GetMapping("/mes-reservations")
    public ResponseEntity<List<ReservationResponseDTO>> getMesReservations(Authentication authentication) {
        String usersEmail = authentication.getName();
        Users users = userRepository.findByEmail(usersEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        List<Reservation> reservations = reservationRepository
                .findByUsersIdOrderByDateDebutDesc(users.getId());

        List<ReservationResponseDTO> response = reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDTO> getReservationById(
            @PathVariable Long id,
            Authentication authentication) {
        String usersEmail = authentication.getName();
        Users users = userRepository.findByEmail(usersEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation non trouvee"));

        if (!reservation.getUsers().getId().equals(users.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(convertToDTO(reservation));
    }

    /**
     * Renvoie la progression d'une reservation en deux volets : experience
     * voyageur (etapesSejour) et suivi administratif Pastell.
     */
    @GetMapping("/{id}/timeline")
    public ResponseEntity<ReservationTimelineDTO> getTimeline(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        Reservation reservation = reservationRepository.findById(id)
                .orElse(null);

        if (reservation == null) {
            return ResponseEntity.notFound().build();
        }

        if (!reservation.getUsers().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        Optional<PastellSync> syncOpt = pastellSyncRepository.findByReservationId(id);
        PastellSync sync = syncOpt.orElse(null);

        List<TimelineEtapeDTO> etapesSejour = buildEtapesSejour(reservation);
        SuiviAdministratif suiviAdmin = buildSuiviAdministratif(sync);

        return ResponseEntity.ok(new ReservationTimelineDTO(
                reservation.getId(),
                reservation.getStatut().name(),
                etapesSejour,
                suiviAdmin
        ));
    }

    /**
     * Annule une reservation appartenant a l'utilisateur authentifie.
     * <p>
     * Regle metier appliquee (Lot 3) : une reservation n'est annulable que
     * si elle est strictement "a venir", c'est-a-dire que la date du jour
     * est anterieure a sa date de debut. Les cas bloques :
     * <ul>
     *   <li>Reservation deja annulee : conflit, message d'idempotence</li>
     *   <li>Reservation au statut TERMINEE : sejour cloture</li>
     *   <li>Reservation en cours (today entre dateDebut et dateFin)</li>
     *   <li>Reservation passee (today posterieure a dateFin)</li>
     * </ul>
     * Tous ces cas renvoient un 409 Conflict avec un message lisible cote
     * front. Le 204 No Content reste reserve au succes effectif.
     *
     * @param id             id de la reservation a annuler
     * @param authentication identite extraite du JWT
     * @return 204 si succes, 403 si pas le proprietaire, 404 si introuvable, 409 si non annulable
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> annulerReservation(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();
        Users users = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation non trouvee"));

        if (!reservation.getUsers().getId().equals(users.getId())) {
            return ResponseEntity.status(403).build();
        }

        // Verification de la regle metier : annulable uniquement si a venir
        Optional<Map<String, String>> blocage = verifierAnnulabilite(reservation);
        if (blocage.isPresent()) {
            return ResponseEntity.status(409).body(blocage.get());
        }

        reservation.setStatut(Reservation.StatutReservation.ANNULEE);
        reservationRepository.save(reservation);

        return ResponseEntity.noContent().build();
    }

    /**
     * Verifie si une reservation peut etre annulee. Renvoie un Optional
     * vide si l'annulation est permise, ou un body d'erreur pret a etre
     * renvoye en 409 sinon.
     * <p>
     * Cette methode est extraite pour rendre la regle metier testable
     * unitairement et pour qu'elle puisse etre reutilisee dans d'autres
     * endpoints (admin, employe) sans duplication.
     */
    private Optional<Map<String, String>> verifierAnnulabilite(Reservation reservation) {
        Reservation.StatutReservation statut = reservation.getStatut();

        if (statut == Reservation.StatutReservation.ANNULEE) {
            return Optional.of(erreurAnnulation(
                    "deja_annulee",
                    "Cette reservation est deja annulee."
            ));
        }

        if (statut == Reservation.StatutReservation.TERMINEE) {
            return Optional.of(erreurAnnulation(
                    "sejour_termine",
                    "Le sejour est termine, vous ne pouvez plus annuler cette reservation."
            ));
        }

        LocalDate aujourdhui = LocalDate.now();
        LocalDate dateDebut = reservation.getDateDebut();
        LocalDate dateFin = reservation.getDateFin();

        if (dateDebut == null) {
            // Securite : si les dates manquent, on laisse passer pour ne pas
            // bloquer une reservation legitime sur une donnee incoherente.
            return Optional.empty();
        }

        // La reservation est "a venir" uniquement si today est strictement
        // anterieure a dateDebut. Le jour J est deja considere comme
        // un sejour commence (on a reserve la nuit du dateDebut).
        boolean estAVenir = aujourdhui.isBefore(dateDebut);
        if (estAVenir) {
            return Optional.empty();
        }

        boolean estPassee = dateFin != null && aujourdhui.isAfter(dateFin);
        String message = estPassee
                ? "Cette reservation est passee. Annulation impossible."
                : "Votre sejour a deja commence. Annulation impossible.";

        return Optional.of(erreurAnnulation(
                estPassee ? "sejour_passe" : "sejour_en_cours",
                message
        ));
    }

    private Map<String, String> erreurAnnulation(String code, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", code);
        body.put("message", message);
        return body;
    }

    /**
     * Construit la timeline experience voyageur en 4 etapes.
     * <p>
     * Logique de calcul, basee sur le statut et les dates :
     * <ul>
     *   <li>Si statut ANNULEE : etape 1 ERROR, le reste PENDING</li>
     *   <li>Si statut EN_ATTENTE : etape 1 CURRENT, le reste PENDING</li>
     *   <li>Si statut CONFIRMEE :
     *     <ul>
     *       <li>Avant J-7 : etape 1 DONE, etape 2 PENDING</li>
     *       <li>Entre J-7 et J-1 : etape 1 DONE, etape 2 CURRENT</li>
     *       <li>Pendant le sejour : 1 et 2 DONE, etape 3 CURRENT</li>
     *       <li>Apres dateFin : 1, 2 et 3 DONE, etape 4 CURRENT</li>
     *     </ul>
     *   </li>
     *   <li>Si statut TERMINEE : toutes les etapes DONE</li>
     * </ul>
     */
    private List<TimelineEtapeDTO> buildEtapesSejour(Reservation reservation) {
        Reservation.StatutReservation statut = reservation.getStatut();
        LocalDate aujourdhui = LocalDate.now();
        LocalDate dateDebut = reservation.getDateDebut();
        LocalDate dateFin = reservation.getDateFin();

        // Date de creation pour l'etape 1
        Instant dateCreation = reservation.getDateCreation() != null
                ? reservation.getDateCreation().toInstant(ZoneOffset.UTC)
                : null;

        // Cas particulier : reservation annulee
        if (statut == Reservation.StatutReservation.ANNULEE) {
            return List.of(
                    new TimelineEtapeDTO(1, "Reservation annulee", "ERROR", dateCreation),
                    new TimelineEtapeDTO(2, "Preparation de votre arrivee", "PENDING", null),
                    new TimelineEtapeDTO(3, "Sejour en cours", "PENDING", null),
                    new TimelineEtapeDTO(4, "Sejour termine", "PENDING", null)
            );
        }

        // Cas particulier : reservation terminee
        if (statut == Reservation.StatutReservation.TERMINEE) {
            Instant fin = dateFin != null
                    ? dateFin.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    : null;
            return List.of(
                    new TimelineEtapeDTO(1, "Reservation confirmee", "DONE", dateCreation),
                    new TimelineEtapeDTO(2, "Preparation de votre arrivee", "DONE", null),
                    new TimelineEtapeDTO(3, "Sejour en cours", "DONE", null),
                    new TimelineEtapeDTO(4, "Sejour termine", "DONE", fin)
            );
        }

        // Cas particulier : reservation non encore confirmee
        if (statut != Reservation.StatutReservation.CONFIRMEE) {
            return List.of(
                    new TimelineEtapeDTO(1, "Reservation confirmee", "CURRENT", null),
                    new TimelineEtapeDTO(2, "Preparation de votre arrivee", "PENDING", null),
                    new TimelineEtapeDTO(3, "Sejour en cours", "PENDING", null),
                    new TimelineEtapeDTO(4, "Sejour termine", "PENDING", null)
            );
        }

        // Cas confirmee : on calcule l'etape courante selon les dates
        List<TimelineEtapeDTO> etapes = new ArrayList<>();
        etapes.add(new TimelineEtapeDTO(1, "Reservation confirmee", "DONE", dateCreation));

        if (dateDebut == null || dateFin == null) {
            // Securite : si les dates manquent, on s'arrete la
            etapes.add(new TimelineEtapeDTO(2, "Preparation de votre arrivee", "PENDING", null));
            etapes.add(new TimelineEtapeDTO(3, "Sejour en cours", "PENDING", null));
            etapes.add(new TimelineEtapeDTO(4, "Sejour termine", "PENDING", null));
            return etapes;
        }

        long joursAvantArrivee = ChronoUnit.DAYS.between(aujourdhui, dateDebut);
        boolean enSejour = !aujourdhui.isBefore(dateDebut) && !aujourdhui.isAfter(dateFin);
        boolean sejourPasse = aujourdhui.isAfter(dateFin);

        // Etape 2 : preparation
        String etape2Statut;
        if (sejourPasse || enSejour) {
            etape2Statut = "DONE";
        } else if (joursAvantArrivee <= JOURS_AVANT_PREPARATION) {
            etape2Statut = "CURRENT";
        } else {
            etape2Statut = "PENDING";
        }
        etapes.add(new TimelineEtapeDTO(2, "Preparation de votre arrivee", etape2Statut, null));

        // Etape 3 : sejour en cours
        String etape3Statut;
        Instant etape3Date = null;
        if (sejourPasse) {
            etape3Statut = "DONE";
        } else if (enSejour) {
            etape3Statut = "CURRENT";
            etape3Date = dateDebut.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } else {
            etape3Statut = "PENDING";
        }
        etapes.add(new TimelineEtapeDTO(3, "Sejour en cours", etape3Statut, etape3Date));

        // Etape 4 : sejour termine
        String etape4Statut = sejourPasse ? "CURRENT" : "PENDING";
        etapes.add(new TimelineEtapeDTO(4, "Sejour termine", etape4Statut, null));

        return etapes;
    }

    /**
     * Construit le bloc d'information sur le dossier Pastell.
     * <p>
     * Si pas de PastellSync (jamais synchronise), on renvoie un statut
     * d'attente avec un message rassurant. Si erreur ou divergence, on
     * leve le flag enErreur pour que le front affiche un bandeau.
     */
    private SuiviAdministratif buildSuiviAdministratif(PastellSync sync) {
        if (sync == null) {
            return new SuiviAdministratif(
                    "EN_ATTENTE",
                    "Votre dossier est en cours de traitement administratif",
                    false,
                    null
            );
        }

        SyncStatus syncStatus = sync.getSyncStatus();
        Instant derniereSynchro = sync.getDerniereSynchro() != null
                ? sync.getDerniereSynchro().toInstant(ZoneOffset.UTC)
                : null;

        boolean enErreur = syncStatus == SyncStatus.EN_ERREUR
                || syncStatus == SyncStatus.DIVERGENCE;

        String statutPastell = syncStatus != null ? syncStatus.name() : "INCONNU";
        String message = messageClient(syncStatus);

        return new SuiviAdministratif(statutPastell, message, enErreur, derniereSynchro);
    }

    /**
     * Traduit le statut technique Pastell en un message court adapte au
     * client final. Pas de jargon technique, pas de mention de retries
     * ou de codes HTTP.
     */
    private String messageClient(SyncStatus syncStatus) {
        if (syncStatus == null) {
            return "Statut administratif en cours d'evaluation";
        }
        return switch (syncStatus) {
            case OK -> "Votre dossier a ete pris en charge avec succes";
            case PENDING -> "Votre dossier est en cours de soumission";
            case EN_RETRY -> "Une nouvelle tentative de soumission est en cours";
            case EN_ERREUR -> "Une difficulte est survenue, nous revenons vers vous sous 48h";
            case DIVERGENCE -> "Votre dossier est en cours de verification par notre equipe";
        };
    }

    private ReservationResponseDTO convertToDTO(Reservation reservation) {
        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .userId(reservation.getUsers() != null
                        ? reservation.getUsers().getId()
                        : null)
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
                .prixTotal(reservation.getPrixTotal() != null
                        ? BigDecimal.valueOf(reservation.getPrixTotal())
                        : BigDecimal.ZERO)
                .statut(reservation.getStatut())
                .codeConfirmation(reservation.getCodeConfirmation())
                .build();
    }
}