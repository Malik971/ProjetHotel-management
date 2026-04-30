package com.example.springhotel.reservation.event;

/**
 * Evenement de domaine publie quand une reservation vient d'etre creee et persistee.
 *
 * Pourquoi un evenement plutot qu'un appel direct a PastellSyncService ?
 *   - Decouplage : ReservationService ne sait rien de Pastell. Si demain on
 *     ajoute d'autres reactions a la creation d'une reservation (notification
 *     analytics, mise a jour d'un cache, audit log...), elles ecouteront le
 *     meme evenement sans modifier ReservationService.
 *   - Testabilite : on peut tester ReservationService sans avoir a mocker
 *     Pastell. On verifie juste que l'evenement est publie.
 *   - Open/Closed Principle : ouvert a l'extension (nouveaux listeners),
 *     ferme a la modification (ReservationService ne change pas).
 *
 * Pourquoi un record ?
 *   - Immuable par construction : un evenement deja publie ne doit jamais
 *     etre modifie par un consommateur.
 *   - Concis, pas de boilerplate.
 *   - Coherent avec le reste du projet (DTOs Pastell, etc.).
 *
 * Pourquoi seulement l'ID, pas l'entite Reservation entiere ?
 *   - Eviter les problemes de lazy-loading : si l'evenement est consomme
 *     hors de la transaction d'origine (ce sera le cas avec
 *     {@code @TransactionalEventListener(AFTER_COMMIT)} au Paquet 4),
 *     les relations LAZY de Reservation seraient detachees et leveraient
 *     des LazyInitializationException.
 *   - Garder l'evenement leger et serialisable : utile si demain on bascule
 *     sur un broker externe (RabbitMQ, Kafka) pour la propagation inter-services.
 *   - Le listener est responsable de recharger la reservation s'il en a besoin,
 *     dans sa propre transaction.
 *
 * @param reservationId identifiant de la reservation nouvellement creee.
 *                      Garantie d'etre non-null et persistee en base au moment
 *                      de la publication de l'evenement.
 */
public record ReservationCreatedEvent(Long reservationId) {

    /**
     * Constructeur compact qui valide l'invariant : on ne publie jamais
     * un evenement avec un id null. Si ca arrive, c'est un bug du
     * code appelant et il faut le faire echouer immediatement.
     */
    public ReservationCreatedEvent {
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId ne peut pas etre null");
        }
    }
}