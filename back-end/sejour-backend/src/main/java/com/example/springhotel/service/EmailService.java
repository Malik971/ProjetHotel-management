package com.example.springhotel.service;

import com.example.springhotel.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void envoyerEmailConfirmation(Reservation reservation) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(reservation.getEmailClient());
            helper.setSubject("Confirmation de réservation - " + reservation.getChambre().getHotel().getNom());
            helper.setText(construireEmailHTML(reservation), true);

            mailSender.send(message);
            System.out.println("✅ Email envoyé à : " + reservation.getEmailClient());
        } catch (MessagingException e) {
            System.err.println("❌ Erreur envoi email : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String construireEmailHTML(Reservation reservation) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .info-box { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .code { background: #667eea; color: white; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; border-radius: 8px; margin: 20px 0; letter-spacing: 2px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Réservation Confirmée !</h1>
                    </div>
                    <div class="content">
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Nous avons le plaisir de confirmer votre réservation.</p>
                        <div class="code">CODE : %s</div>
                        <p>Prix total : %.2f €</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                reservation.getNomClient(),
                reservation.getCodeConfirmation(),
                reservation.getPrixTotal()
        );
    }
}