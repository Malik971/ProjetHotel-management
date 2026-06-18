package com.example.springhotel.reservation.listener;

import com.example.springhotel.entity.Reservation;
import com.example.springhotel.repository.ReservationRepository;
import com.example.springhotel.reservation.event.StatutChangeEvent;
import com.example.springhotel.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envoie un email au client apres chaque transition de statut persistee en base.
 *
 * Meme philosophie que ReservationCreatedListener :
 *   - @TransactionalEventListener AFTER_COMMIT : l'email n'est envoye que si
 *     la transition est reellement committee. Pas d'email pour un rollback.
 *   - Delegation immediate a EmailService : ce listener ne contient aucune
 *     logique metier.
 *   - Exception avalee en dernier recours : un probleme SMTP ne doit jamais
 *     faire echouer le workflow de validation.
 */
@Component
@RequiredArgsConstructor
public class StatutChangeListener {

    private static final Logger log = LoggerFactory.getLogger(StatutChangeListener.class);

    private final ReservationRepository reservationRepository;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatutChange(StatutChangeEvent event) {
        log.debug("StatutChangeEvent recu : reservation {} -> {}",
                event.reservationId(), event.nouveauStatut());

        Reservation reservation = reservationRepository
                .findById(event.reservationId())
                .orElse(null);

        if (reservation == null) {
            log.warn("StatutChangeListener : reservation {} introuvable, email non envoye",
                    event.reservationId());
            return;
        }

        try {
            emailService.envoyerEmailChangementStatut(reservation, event.nouveauStatut());
        } catch (Exception e) {
            log.error("StatutChangeListener : echec email pour reservation {} -> {} : {}",
                    event.reservationId(), event.nouveauStatut(), e.getMessage());
        }
    }
}