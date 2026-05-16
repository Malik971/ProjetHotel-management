package com.example.springhotel.dto;

import java.time.Instant;
import java.util.List;

/**
 * Reponse de GET /api/client/reservations/{id}/timeline.
 * <p>
 * Cette reponse contient DEUX niveaux d'information :
 * <ul>
 *   <li>etapesSejour : timeline en 4 etapes du point de vue du voyageur
 *       (reservation confirmee, preparation, sejour en cours, sejour termine),
 *       calculee a partir du statut et des dates de la reservation</li>
 *   <li>suiviAdministratif : etat du dossier dans le parapheur electronique
 *       Pastell, en vocabulaire neutre pour le client. Ce bloc est toujours
 *       present, et le front l'affiche dans une section collapsible</li>
 * </ul>
 * <p>
 * La separation des deux blocs reflete la philosophie de Pastell chez les
 * collectivites : l'usager voit son experience metier, le suivi technique
 * est implicite et accessible aux curieux. L'admin a sa propre vue technique
 * dediee via /api/admin/pastell-sync/**.
 *
 * @param reservationId       id de la reservation
 * @param statut              statut metier (EN_ATTENTE, CONFIRMEE, etc.)
 * @param etapesSejour        timeline orientee experience voyageur
 * @param suiviAdministratif  etat du dossier Pastell, en libelle client
 */
public record ReservationTimelineDTO(
        Long reservationId,
        String statut,
        List<TimelineEtapeDTO> etapesSejour,
        SuiviAdministratif suiviAdministratif
) {

    /**
     * Une etape de la timeline voyageur.
     *
     * @param ordre   position dans la timeline (1, 2, 3, 4)
     * @param label   libelle affiche a l'utilisateur (vocabulaire voyageur)
     * @param statut  DONE | CURRENT | PENDING | ERROR
     * @param date    date de passage a cet etat, null si pas encore atteint
     */
    public record TimelineEtapeDTO(
            int ordre,
            String label,
            String statut,
            Instant date
    ) {
    }

    /**
     * Bloc d'information sur le dossier administratif Pastell.
     * <p>
     * Le front l'affiche dans une section collapsible avec un libelle du type
     * "Suivi administratif". On y precise que le dossier passe par un parapheur
     * electronique conforme au secteur public, sans noyer le client de details
     * techniques.
     *
     * @param statutPastell     etat technique cote Pastell (CREATION, EN_ATTENTE_VALIDATION, VALIDEE, etc.)
     * @param message           libelle court adapte au client final
     * @param enErreur          true si le dossier necessite une attention particuliere
     * @param derniereSynchro   date de la derniere synchronisation avec Pastell, null si jamais synchronise
     */
    public record SuiviAdministratif(
            String statutPastell,
            String message,
            boolean enErreur,
            Instant derniereSynchro
    ) {
    }
}