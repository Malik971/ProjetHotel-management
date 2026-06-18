package com.example.springhotel.service;

import com.example.springhotel.dto.SignatureRequestDTO;
import com.example.springhotel.dto.SignatureResponseDTO;
import com.example.springhotel.entity.Reservation;
import com.example.springhotel.entity.Reservation.StatutReservation;
import com.example.springhotel.repository.ReservationRepository;
import com.example.springhotel.reservation.event.StatutChangeEvent;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Service de signature electronique - implementation niveau 2 (locale).
 *
 * Responsabilites :
 *   1. Valider que la transition est autorisee (EN_ATTENTE ou SIGNATURE_EN_COURS -> SIGNATURE_APPOSEE)
 *   2. Generer un PDF recepisse avec les donnees de la reservation et l'image
 *      de signature fournie par le canvas HTML5
 *   3. Persister le PDF, le nom du signataire et l'horodatage sur la reservation
 *   4. Faire passer la reservation a CONFIRMEE apres signature (auto-transition)
 *   5. Publier StatutChangeEvent pour chaque transition
 *
 * POINT DE MIGRATION NIVEAU 3 :
 *   Au niveau 3 (mock parapheur distant), la methode signerReservation() est
 *   remplacee par un appel REST vers le mock :
 *     POST http://mock-parapheur/api/signer
 *       body : { reservationId, signatureBase64, nomSignataire }
 *     reponse : { pdfBase64, transactionId, signedAt }
 *   Le reste de cette classe (persistance, publication d'evenement, transition
 *   vers CONFIRMEE) reste identique. Le changement est borne a ce service.
 */
@Service
@RequiredArgsConstructor
public class SignatureService {

    private static final Logger log = LoggerFactory.getLogger(SignatureService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Initie la signature d'une reservation : passe son statut a SIGNATURE_EN_COURS.
     *
     * Appele quand l'admin ouvre la page de signature. Permet de tracer qu'un agent
     * a commence a traiter le dossier, meme si la signature n'est pas encore apposee.
     * Au niveau 3, cette methode enverrait le document au parapheur et retournerait
     * l'identifiant de la session de signature.
     *
     * @throws IllegalStateException si la reservation n'est pas en EN_ATTENTE
     */
    @Transactional
    public void initierSignature(Long reservationId) {
        Reservation reservation = charger(reservationId);

        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new IllegalStateException(
                    "Impossible d'initier la signature : statut actuel = "
                            + reservation.getStatut() + ", attendu = EN_ATTENTE");
        }

        StatutReservation ancien = reservation.getStatut();
        reservation.setStatut(StatutReservation.SIGNATURE_EN_COURS);
        reservationRepository.save(reservation);

        log.info("Signature initiee pour reservation {}", reservationId);
        eventPublisher.publishEvent(new StatutChangeEvent(reservationId, ancien, StatutReservation.SIGNATURE_EN_COURS));
    }

    /**
     * Apposer la signature : genere le PDF, persiste, passe a SIGNATURE_APPOSEE
     * puis auto-transitionne a CONFIRMEE.
     *
     * POINT DE MIGRATION NIVEAU 3 : remplacer genererPdfSigne() par un appel
     * REST vers le mock parapheur. Le reste de la methode (persistance, evenements)
     * ne change pas.
     *
     * @param reservationId  id de la reservation a signer
     * @param request        contient signatureBase64 et nomSignataire
     * @return SignatureResponseDTO avec le statut final et les metadonnees
     * @throws IllegalStateException si la reservation n'est pas en SIGNATURE_EN_COURS
     */
    @Transactional
    public SignatureResponseDTO signerReservation(Long reservationId, SignatureRequestDTO request) {
        Reservation reservation = charger(reservationId);

        if (reservation.getStatut() != StatutReservation.SIGNATURE_EN_COURS
                && reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new IllegalStateException(
                    "Impossible de signer : statut actuel = "
                            + reservation.getStatut()
                            + ", attendu = EN_ATTENTE ou SIGNATURE_EN_COURS");
        }

        // --- NIVEAU 2 : generation locale du PDF ---------------------------------
        // --- MIGRATION NIVEAU 3 : remplacer ce bloc par un appel REST ----------
        String pdfBase64 = genererPdfSigne(reservation, request.signatureBase64(), request.nomSignataire());
        // -------------------------------------------------------------------------

        LocalDateTime signedAt = LocalDateTime.now();
        StatutReservation ancienStatut = reservation.getStatut();

        // Persister la signature
        reservation.setSignaturePdfBase64(pdfBase64);
        reservation.setNomSignataire(request.nomSignataire());
        reservation.setSignedAt(signedAt);
        reservation.setStatut(StatutReservation.SIGNATURE_APPOSEE);
        reservationRepository.save(reservation);

        log.info("Signature apposee sur reservation {} par {}", reservationId, request.nomSignataire());
        eventPublisher.publishEvent(new StatutChangeEvent(reservationId, ancienStatut, StatutReservation.SIGNATURE_APPOSEE));

        // Auto-transition vers CONFIRMEE
        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservationRepository.save(reservation);

        log.info("Reservation {} passee a CONFIRMEE apres signature", reservationId);
        eventPublisher.publishEvent(new StatutChangeEvent(reservationId, StatutReservation.SIGNATURE_APPOSEE, StatutReservation.CONFIRMEE));

        return new SignatureResponseDTO(
                reservationId,
                StatutReservation.CONFIRMEE,
                request.nomSignataire(),
                signedAt,
                true
        );
    }

    // -------------------------------------------------------------------
    // Generation PDF locale (niveau 2)
    //
    // Produit un recepisse A4 avec :
    //   - En-tete color : nom de l'hotel et reference dossier
    //   - Tableau recapitulatif des informations de la reservation
    //   - Image de la signature apposee avec nom et horodatage
    //   - Pied de page : mention legale
    //
    // Le base64 retourne est stocke dans reservation.signaturePdfBase64.
    // Un endpoint GET /api/admin/reservations/{id}/pdf permet de le
    // telecharger depuis le dashboard.
    // -------------------------------------------------------------------

    private String genererPdfSigne(Reservation r, String signatureBase64, String nomSignataire) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 60, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            String hotel = r.getChambre() != null && r.getChambre().getHotel() != null
                    ? r.getChambre().getHotel().getNom()
                    : "SpringHotel";

            Font fontTitre   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
            Font fontSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(3, 105, 161));
            Font fontLabel   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 116, 139));
            Font fontValeur  = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font fontFooter  = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(148, 163, 184));

            // En-tete
            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            PdfPCell cellHeader = new PdfPCell(new Phrase(hotel + " - Recepisse de reservation", fontTitre));
            cellHeader.setBackgroundColor(new Color(14, 165, 233));
            cellHeader.setPadding(16);
            cellHeader.setBorder(Rectangle.NO_BORDER);
            cellHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.addCell(cellHeader);
            document.add(header);
            document.add(new Paragraph(" "));

            // Reference
            Paragraph ref = new Paragraph("Dossier N " + r.getCodeConfirmation()
                    + "   |   Cree le " + r.getDateCreation().format(FMT), fontLabel);
            ref.setAlignment(Element.ALIGN_RIGHT);
            document.add(ref);
            document.add(new Paragraph(" "));

            // Section informations reservation
            document.add(new Paragraph("Informations de la reservation", fontSection));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2f});

            ajouterLigne(table, "Client",       r.getNomClient(),        fontLabel, fontValeur);
            ajouterLigne(table, "Email",         r.getEmailClient(),      fontLabel, fontValeur);
            ajouterLigne(table, "Telephone",     r.getTelephoneClient(),  fontLabel, fontValeur);
            ajouterLigne(table, "Chambre",       r.getChambre() != null ? r.getChambre().getNom() : "-", fontLabel, fontValeur);
            ajouterLigne(table, "Arrivee",       r.getDateDebut() != null ? r.getDateDebut().format(FMT_DATE) : "-", fontLabel, fontValeur);
            ajouterLigne(table, "Depart",        r.getDateFin()   != null ? r.getDateFin().format(FMT_DATE)   : "-", fontLabel, fontValeur);
            ajouterLigne(table, "Personnes",     String.valueOf(r.getNombrePersonnes()), fontLabel, fontValeur);
            ajouterLigne(table, "Prix total",    String.format("%.2f EUR", r.getPrixTotal()), fontLabel, fontValeur);
            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Section signature
            document.add(new Paragraph("Visa de validation", fontSection));
            document.add(new Paragraph(" "));

            // Image de signature
            if (signatureBase64 != null && !signatureBase64.isBlank()) {
                String b64 = signatureBase64.contains(",")
                        ? signatureBase64.split(",", 2)[1]
                        : signatureBase64;
                byte[] imgBytes = Base64.getDecoder().decode(b64);
                Image img = Image.getInstance(imgBytes);
                img.scaleToFit(200, 80);
                img.setAlignment(Element.ALIGN_LEFT);
                document.add(img);
            }

            Paragraph signataire = new Paragraph(
                    "Signe par : " + nomSignataire + "   |   Le " + LocalDateTime.now().format(FMT),
                    fontLabel
            );
            document.add(signataire);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Pied de page
            Paragraph footer = new Paragraph(
                    "Ce document est un recepisse genere automatiquement. "
                            + "Il constitue la preuve de la validation administrative du dossier.",
                    fontFooter
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (Exception e) {
            log.error("Echec generation PDF pour reservation {} : {}", r.getId(), e.getMessage());
            // On retourne null : le flag pdfDisponible=false dans la reponse
            // indiquera au frontend que le PDF n'est pas disponible, sans
            // bloquer la confirmation.
            return null;
        }
    }

    private void ajouterLigne(PdfPTable table, String label, String valeur,
                              Font fontLabel, Font fontValeur) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, fontLabel));
        cellLabel.setBackgroundColor(new Color(248, 250, 252));
        cellLabel.setPadding(8);
        cellLabel.setBorderColor(new Color(226, 232, 240));

        PdfPCell cellValeur = new PdfPCell(new Phrase(valeur != null ? valeur : "-", fontValeur));
        cellValeur.setPadding(8);
        cellValeur.setBorderColor(new Color(226, 232, 240));

        table.addCell(cellLabel);
        table.addCell(cellValeur);
    }

    private Reservation charger(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation introuvable : id=" + reservationId));
    }
}