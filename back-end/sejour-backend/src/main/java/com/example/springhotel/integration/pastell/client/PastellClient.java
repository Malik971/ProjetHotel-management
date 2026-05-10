package com.example.springhotel.integration.pastell.client;

import com.example.springhotel.integration.pastell.config.PastellConfig;
import com.example.springhotel.integration.pastell.config.PastellProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;

/**
 * Client HTTP type pour les appels sortants vers Pastell.
 * <p>
 * Responsabilites :
 * </p>
 *   - Construire les requetes HTTP conformes au protocole Pastell (form-data en entree,
 *     JSON en sortie, base path /api/v2/...).<br>
 *   - Convertir les reponses Pastell en objets Java exploitables (records DTOs).<br>
 *   - Convertir TOUS les echecs (HTTP 4xx/5xx, timeouts, erreurs reseau) en
 *     {@link PastellApiException} typee, pour que le code metier appelant n'ait
 *     a connaitre qu'un seul type d'exception.
 * <p>
 * Ce que ce client NE fait PAS :
 * </p>
 *   - Aucune logique metier : il ne sait pas ce qu'est une reservation, ni un PastellSync.
 *     Il prend des donnees primitives et retourne des DTOs. Le mapping metier est la
 *     responsabilite des services (PastellSyncService pour la sync montante au Lot 3,
 *     PastellInboundSyncService pour la sync descendante au Lot 5).<br>
 *   - Aucun retry : un appel = une tentative. Le retry sur la sync montante a ete
 *     ajoute au Lot 4 via PastellClientWithRetry qui wrap ce client. Pour la sync
 *     descendante (Lot 5), on accepte un appel = une tentative : si un poll echoue,
 *     le suivant 30s plus tard rattrappera (effet de retry naturel par la frequence).<br>
 *   - Aucune persistance : il ne touche jamais a la base. Pure couche de transport.
 * <p>
 * Conditional bean :
 *   Comme {@link PastellConfig}, ce composant n'est instancie que si
 *   {@code pastell.enabled=true}.
 * <p>
 * Analogie pedagogique :
 *   PastellClient est l'employe de l'accueil qui transmet des notes au siege
 *   (Pastell) et qui releve aussi le courrier (lecture du journal). Il ne sait
 *   rien des dossiers clients, il sait juste comment ecrire et lire dans le bon
 *   format. Le metier reste dans le bureau (les services).
 */
@Component
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellClient {

    private static final Logger log = LoggerFactory.getLogger(PastellClient.class);

    /**
     * Nom du parametre form-data attendu par Pastell pour le type de dossier.
     */
    private static final String FORM_FIELD_TYPE = "type";

    /**
     * ParameterizedTypeReference pour deserialiser le tableau JSON du journal
     * en List<PastellJournalEntry>. Sans ce wrapper, le parameterized type est
     * efface a la compilation et Jackson ne peut pas savoir qu'on veut une liste.
     */
    private static final ParameterizedTypeReference<List<PastellJournalEntry>> JOURNAL_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final PastellProperties properties;

    public PastellClient(
            @Qualifier(PastellConfig.PASTELL_REST_CLIENT) RestClient restClient,
            PastellProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    // ============================================================
    // SYNC MONTANTE (Lot 3) : creation de dossier
    // ============================================================

    /**
     * Cree un nouveau dossier Pastell vide pour une reservation.
     * <p>
     * Format de la requete :
     * <pre>
     *   POST /api/v2/entite/{entiteId}/document
     *   Content-Type: multipart/form-data
     *   Authorization: Basic ... (ajoute par l'interceptor du RestClient)
     *
     *   ------boundary
     *   Content-Disposition: form-data; name="type"
     *
     *   reservation-hoteliere
     *   ------boundary--
     * </pre>
     *
     * @return la reponse Pastell contenant l'id_d du dossier nouvellement cree
     * @throws PastellApiException en cas d'echec HTTP (4xx, 5xx) ou technique (timeout, reseau)
     */
    public PastellCreateDocumentResponse createDocument() {
        long entiteId = properties.getEntiteId();
        String typeDossier = properties.getTypeDossier();

        log.debug("Pastell create-document : entiteId={}, type={}", entiteId, typeDossier);

        MultiValueMap<String, Object> formBody = new LinkedMultiValueMap<>();
        formBody.add(FORM_FIELD_TYPE, typeDossier);

        try {
            PastellCreateDocumentResponse response = restClient.post()
                    .uri("/api/v2/entite/{entiteId}/document", entiteId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formBody)
                    .retrieve()
                    .body(PastellCreateDocumentResponse.class);

            // Defense en profondeur : si Pastell renvoie un 2xx mais avec un body vide
            // ou un id_d null, c'est anormal et on doit le signaler comme une erreur.
            if (response == null || response.idD() == null || response.idD().isBlank()) {
                throw new PastellApiException(
                        200,
                        response == null ? null : response.toString(),
                        "Pastell a repondu 2xx mais sans id_d exploitable"
                );
            }

            log.debug("Pastell create-document succes : id_d={}", response.idD());
            return response;

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            log.warn("Pastell create-document echec HTTP {} : {}", status, body);
            throw new PastellApiException(
                    status,
                    body,
                    "Pastell a repondu en erreur HTTP " + status
            );

        } catch (ResourceAccessException e) {
            log.warn("Pastell create-document echec reseau : {}", e.getMessage());
            throw new PastellApiException(
                    "Echec d'acces a Pastell (timeout ou reseau) : " + e.getMessage(),
                    e
            );

        } catch (RestClientException e) {
            log.warn("Pastell create-document erreur RestClient : {}", e.getMessage());
            throw new PastellApiException(
                    "Erreur lors de l'appel Pastell : " + e.getMessage(),
                    e
            );
        }
    }

    // ============================================================
    // SYNC DESCENDANTE (Lot 5) : lecture du journal
    // ============================================================

    /**
     * Recupere les entrees du journal Pastell dont l'idJ est strictement superieur
     * a la borne fournie.
     *<p>
     * Forme de la requete :
     * <pre>
     *   GET /api/v2/journal?since_id_j={sinceIdJ}
     *   Authorization: Basic ...
     * </pre>
     * <p>
     * Forme attendue de la reponse :
     * </p>
     * <pre>
     *   [
     *     {"id_j": 42, "id_d": "abc", "id_e": 1, "action": "validee", "date": "2026-04-28 15:30:00"},
     *     {"id_j": 43, "id_d": "xyz", "id_e": 1, "action": "annulee", "date": "2026-04-28 15:32:11"}
     *   ]
     * </pre>
     * <p>
     * Cas vide : Pastell renvoie 200 avec un tableau vide []. On retourne une liste
     * vide, pas null. Une liste vide signifie "rien de neuf depuis ton dernier poll",
     * c'est un cas normal de fonctionnement, pas une erreur.
     *<p>
     * Pourquoi pas de validation defensive sur les champs (comme pour createDocument) ?
     *   - Le mock garantit la forme. En cas de payload corrompu (champ manquant),
     *     Jackson levera une JsonMappingException qui sera attrapee comme RestClientException
     *     et convertie en PastellApiException. Pas besoin de defensive coding supplementaire.
     *   - Si on voulait filtrer les entrees mal formees au cas par cas (ex. idJ <= 0),
     *     ce serait au service appelant de le faire, pas au transport.
     *
     * @param sinceIdJ borne exclusive : seules les entrees avec idJ > sinceIdJ seront retournees.
     *                 Passer 0 pour tout recuperer depuis le debut du journal.
     * @return liste (potentiellement vide) des nouvelles entrees, ordonnees par idJ croissant
     *         (Pastell garantit cet ordre, on n'a pas a le retrier).
     * @throws PastellApiException en cas d'echec HTTP ou reseau
     */
    public List<PastellJournalEntry> fetchJournalSince(long sinceIdJ) {
        log.debug("Pastell get-journal : sinceIdJ={}", sinceIdJ);

        try {
            List<PastellJournalEntry> entries = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v2/journal")
                            .queryParam("since_id_j", sinceIdJ)
                            .build())
                    .retrieve()
                    .body(JOURNAL_LIST_TYPE);

            // Si Pastell renvoie un body vide ou null (pathologique), on traite
            // comme "aucune nouveaute" plutot que de propager un null. Cela evite
            // un NPE cote service appelant.
            if (entries == null) {
                log.warn("Pastell get-journal : body null recu, traite comme liste vide");
                return Collections.emptyList();
            }

            log.debug("Pastell get-journal succes : {} entree(s) recuperee(s)", entries.size());
            return entries;

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            log.warn("Pastell get-journal echec HTTP {} : {}", status, body);
            throw new PastellApiException(
                    status,
                    body,
                    "Pastell a repondu en erreur HTTP " + status + " sur GET /api/v2/journal"
            );

        } catch (ResourceAccessException e) {
            log.warn("Pastell get-journal echec reseau : {}", e.getMessage());
            throw new PastellApiException(
                    "Echec d'acces a Pastell sur GET /api/v2/journal (timeout ou reseau) : " + e.getMessage(),
                    e
            );

        } catch (RestClientException e) {
            log.warn("Pastell get-journal erreur RestClient : {}", e.getMessage());
            throw new PastellApiException(
                    "Erreur lors de l'appel GET /api/v2/journal : " + e.getMessage(),
                    e
            );
        }
    }
}