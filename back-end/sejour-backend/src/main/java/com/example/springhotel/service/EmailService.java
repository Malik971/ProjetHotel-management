package com.example.springhotel.service;

import com.example.springhotel.entity.Reservation;
import com.example.springhotel.entity.Reservation.StatutReservation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;

    public void envoyerEmailChangementStatut(
            Reservation reservation,
            StatutReservation nouveauStatut) {

        String sujet = construireSujet(reservation, nouveauStatut);

        if (sujet == null) {
            return;
        }

        String corps = construireCorps(reservation, nouveauStatut);
        envoyerEmail(reservation.getEmailClient(), sujet, corps);
    }

    private void envoyerEmail(String destinataire, String sujet, String corps) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(corps, true);

            mailSender.send(message);

            log.info("Email '{}' envoye a {}", sujet, destinataire);

        } catch (MessagingException e) {
            log.warn(
                    "Echec envoi email a {} : {}",
                    destinataire,
                    e.getMessage()
            );
        }
    }

    private String construireSujet(
            Reservation reservation,
            StatutReservation statut) {

        String hotel =
                reservation.getChambre() != null
                        && reservation.getChambre().getHotel() != null
                        ? reservation.getChambre().getHotel().getNom()
                        : "SpringHotel";

        return switch (statut) {
            case EN_ATTENTE ->
                    "Votre demande est en cours de traitement - " + hotel;

            case SIGNATURE_APPOSEE ->
                    "Votre dossier est en cours de validation - " + hotel;

            case CONFIRMEE ->
                    "Reservation confirmee ! - " + hotel;

            case ANNULEE ->
                    "Reservation annulee - " + hotel;

            case TERMINEE ->
                    "Merci pour votre sejour - " + hotel;

            default -> null;
        };
    }

    private String construireCorps(
            Reservation reservation,
            StatutReservation statut) {

        String nom =
                reservation.getNomClient() != null
                        ? reservation.getNomClient()
                        : "Client";

        String code =
                reservation.getCodeConfirmation() != null
                        ? reservation.getCodeConfirmation()
                        : "";

        String debut =
                reservation.getDateDebut() != null
                        ? reservation.getDateDebut().format(FMT)
                        : "";

        String fin =
                reservation.getDateFin() != null
                        ? reservation.getDateFin().format(FMT)
                        : "";

        String hotel =
                reservation.getChambre() != null
                        && reservation.getChambre().getHotel() != null
                        ? reservation.getChambre().getHotel().getNom()
                        : "SpringHotel";

        String messageStatut = switch (statut) {

            case EN_ATTENTE ->
                    """
                    <p>Votre demande de reservation a bien ete recue.</p>
                    <p>Un membre de notre equipe va la traiter prochainement.
                    Vous recevrez un email de confirmation une fois la validation effectuee.</p>
                    """;

            case SIGNATURE_APPOSEE ->
                    """
                    <p>Votre dossier a ete instruit par notre equipe et est en cours de confirmation
                    finale. Vous recevrez tres prochainement votre confirmation definitive.</p>
                    """;

            case CONFIRMEE ->
                    """
                    <p>Votre reservation est <strong>confirmee</strong>.</p>
                    <p>Nous vous attendons avec plaisir.</p>
                    """;

            case ANNULEE ->
                    """
                    <p>Votre reservation a ete annulee.</p>
                    <p>Si vous avez des questions, n'hesitez pas a nous contacter.</p>
                    """;

            case TERMINEE ->
                    """
                    <p>Nous esperons que votre sejour s'est passe dans les meilleures conditions.</p>
                    <p>Merci de votre confiance, au plaisir de vous accueillir a nouveau.</p>
                    """;

            default ->
                    "<p>Votre dossier a ete mis a jour.</p>";
        };

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: Arial, sans-serif; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header {
                    background: linear-gradient(135deg, #0ea5e9, #0369a1);
                    color: white;
                    padding: 24px;
                    border-radius: 8px 8px 0 0;
                    text-align: center;
                }
                .content {
                    background: #f8fafc;
                    padding: 24px;
                    border-radius: 0 0 8px 8px;
                }
                .box {
                    background: white;
                    padding: 16px;
                    border-radius: 6px;
                    box-shadow: 0 1px 3px rgba(0,0,0,.08);
                    margin: 16px 0;
                }
                .code {
                    font-size: 22px;
                    font-weight: bold;
                    color: #0ea5e9;
                    text-align: center;
                    letter-spacing: 2px;
                    padding: 12px;
                }
                .label {
                    color: #64748b;
                    font-size: 12px;
                    text-transform: uppercase;
                }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                    <h2>%s</h2>
                </div>

                <div class="content">
                  <p>Bonjour <strong>%s</strong>,</p>

                  %s

                  <div class="box">
                    <p class="label">Code de reference</p>
                    <p class="code">%s</p>

                    <p>
                      <span class="label">Dates :</span>
                      %s &rarr; %s
                    </p>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """
                .formatted(
                        hotel,
                        nom,
                        messageStatut,
                        code,
                        debut,
                        fin
                );
    }
}