package com.example.springhotel.integration.pastell.client;

import com.example.springhotel.integration.pastell.config.PastellConfig;
import com.example.springhotel.integration.pastell.config.PastellProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client HTTP type pour les appels sortants vers Pastell.
 *
 * Responsabilites :
 *   - Construire les requetes HTTP conformes au protocole Pastell (form-data en entree,
 *     JSON en sortie, base path /api/v2/...).
 *   - Convertir les reponses Pastell en objets Java exploitables (records DTOs).
 *   - Convertir TOUS les echecs (HTTP 4xx/5xx, timeouts, erreurs reseau) en
 *     {@link PastellApiException} typee, pour que le code metier appelant n'ait
 *     a connaitre qu'un seul type d'exception.
 *
 * Ce que ce client NE fait PAS :
 *   - Aucune logique metier : il ne sait pas ce qu'est une reservation, ni un PastellSync.
 *     Il prend des donnees primitives (entiteId, type) et retourne un DTO. Le mapping
 *     metier est la responsabilite de PastellSyncService (Paquet 3).
 *   - Aucun retry : un appel = une tentative. Le retry sera ajoute au Lot 4
 *     via Spring Retry, en wrappant ce client. Garder ce client "stupide" facilite
 *     le test isole et la composition des comportements.
 *   - Aucune persistance : il ne touche jamais a la base. Pure couche de transport.
 *
 * Conditional bean :
 *   Comme {@link PastellConfig}, ce composant n'est instancie que si
 *   {@code pastell.enabled=true}. Quand l'integration est desactivee, ce bean
 *   est absent du contexte, ce qui evite toute injection accidentelle dans
 *   un service metier qui devrait fonctionner sans Pastell.
 *
 * Analogie pedagogique :
 *   PastellClient est l'employe de l'accueil qui transmet des notes au siege
 *   (Pastell). Il ne sait rien des dossiers clients (les reservations), il sait
 *   juste comment ecrire une note dans le bon format, l'envoyer par pneumatique,
 *   et signaler si le pneumatique est en panne. Le metier reste dans le bureau
 *   (PastellSyncService).
 */
@Component
@ConditionalOnProperty(name = "pastell.enabled", havingValue = "true")
public class PastellClient {

    private static final Logger log = LoggerFactory.getLogger(PastellClient.class);

    /**
     * Nom du parametre form-data attendu par Pastell pour le type de dossier.
     * Defini comme constante pour eviter la duplication et clarifier le contrat
     * de l'API distante.
     */
    private static final String FORM_FIELD_TYPE = "type";

    private final RestClient restClient;
    private final PastellProperties properties;

    /**
     * Constructeur avec injection explicite du RestClient qualifie.
     *
     * Le {@link Qualifier} est obligatoire ici parce qu'on veut le RestClient
     * dedie Pastell (avec Basic Auth, base URL, interceptor de logging),
     * pas un eventuel autre RestClient generique du contexte.
     */
    public PastellClient(
            @Qualifier(PastellConfig.PASTELL_REST_CLIENT) RestClient restClient,
            PastellProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * Cree un nouveau dossier Pastell vide pour une reservation.
     *
     * Equivalent metier : "ouvrir un dossier de reservation au siege".
     * A ce stade, le dossier est CREE mais VIDE : les champs metier
     * (dates, client, hotel, prix) seront pousses ulterieurement via
     * modify-document (Lot 3+).
     *
     * Cette methode ne prend aucun parametre : tout vient de PastellProperties.
     * Au Lot 3+, quand on aura besoin de pousser les donnees metier, on ajoutera
     * une methode {@code modifyDocument(String idD, Reservation r)} qui prendra
     * la reservation en parametre.
     *
     * Format de la requete envoyee :
     * <pre>
     *   POST /api/v2/entite/{entiteId}/document
     *   Content-Type: multipart/form-data
     *   Authorization: Basic ... (ajoute par l'interceptor du RestClient)
     *   User-Agent: Sejour-Backend/1.0 (Pastell-Integration)
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

        // Construction du body multipart/form-data.
        // MultiValueMap permet plusieurs valeurs par cle, format attendu par Spring
        // pour generer un body multipart correct.
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
            // Sans cette verification, on stockerait silencieusement un PastellSync
            // sans id_d, ce qui casserait l'idempotence des appels suivants.
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
            // 4xx / 5xx : reponse HTTP recue avec statut d'erreur.
            // On a acces au statut et au body, parfait pour le diagnostic.
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            log.warn("Pastell create-document echec HTTP {} : {}", status, body);
            throw new PastellApiException(
                    status,
                    body,
                    "Pastell a repondu en erreur HTTP " + status
            );

        } catch (ResourceAccessException e) {
            // Timeout, refus de connexion, DNS injoignable...
            // Pas de reponse HTTP, donc statusCode = -1 dans l'exception.
            log.warn("Pastell create-document echec reseau : {}", e.getMessage());
            throw new PastellApiException(
                    "Echec d'acces a Pastell (timeout ou reseau) : " + e.getMessage(),
                    e
            );

        } catch (RestClientException e) {
            // Filet de securite : toute autre erreur RestClient
            // (deserialisation cassee, body illisible, etc.)
            log.warn("Pastell create-document erreur RestClient : {}", e.getMessage());
            throw new PastellApiException(
                    "Erreur lors de l'appel Pastell : " + e.getMessage(),
                    e
            );
        }
    }
}