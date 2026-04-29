package com.example.springhotel.integration.pastell.client;

/**
 * Exception levee par {@link PastellClient} quand un appel HTTP a Pastell
 * retourne un statut d'erreur (4xx ou 5xx) ou echoue pour une raison technique
 * (timeout, erreur reseau, body illisible).
 *
 * Pourquoi une exception dediee plutot que de laisser remonter
 * {@code RestClientException} de Spring ?
 *   - On veut que le code appelant (PastellSyncService au Paquet 3) puisse
 *     attraper specifiquement les erreurs Pastell sans embarquer toutes les
 *     erreurs HTTP du framework.
 *   - On capture le statut HTTP et le corps brut de la reponse pour les
 *     logger dans {@code PastellSync.derniereErreur} et permettre un diagnostic
 *     precis sans avoir a parser des stack traces.
 *   - La distinction permet aussi au futur retry du Lot 4 de decider quoi
 *     reessayer : un 401 (mauvais credentials) ne doit JAMAIS etre reessaye,
 *     un 500 ou un timeout OUI.
 *
 * Cas d'usage typique cote service appelant :
 * <pre>
 * try {
 *     var response = pastellClient.createDocument();
 *     // succes : on stocke response.idD()
 * } catch (PastellApiException e) {
 *     log.warn("Echec create-document : statut={} body={}",
 *              e.getStatusCode(), e.getResponseBody());
 *     // bascule du PastellSync en EN_RETRY
 * }
 * </pre>
 *
 * On etend RuntimeException (et pas Exception) parce qu'on ne veut pas
 * polluer toute la chaine d'appel avec des "throws" : c'est le contrat
 * Spring habituel pour les exceptions techniques.
 */
public class PastellApiException extends RuntimeException {

    /**
     * Code HTTP renvoye par Pastell (ou -1 si l'echec est anterieur a la reponse,
     * par exemple un timeout ou une erreur de connexion).
     */
    private final int statusCode;

    /**
     * Corps brut de la reponse Pastell, tel que recu.
     * Peut etre null en cas d'echec reseau (pas de reponse du tout).
     * Tronque a 1000 caracteres a l'instanciation pour eviter de stocker
     * un body monstrueux dans la base ou les logs.
     */
    private final String responseBody;

    /**
     * Constructeur principal pour les erreurs HTTP avec reponse.
     *
     * @param statusCode   le code HTTP renvoye (ex. 401, 500)
     * @param responseBody le corps de la reponse, sera tronque si trop long
     * @param message      un message descriptif pour les logs
     */
    public PastellApiException(int statusCode, String responseBody, String message) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = truncate(responseBody);
    }

    /**
     * Constructeur pour les echecs sans reponse HTTP (timeout, erreur reseau, etc.).
     *
     * @param message un message descriptif pour les logs
     * @param cause   l'exception sous-jacente (IOException, ResourceAccessException, ...)
     */
    public PastellApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Indique si l'echec est posterieur a une reponse HTTP recue
     * (true) ou si c'est un echec technique anterieur a toute reponse (false).
     */
    public boolean hasHttpResponse() {
        return statusCode >= 0;
    }

    /**
     * Tronque le corps de la reponse a 1000 caracteres pour eviter de polluer
     * les logs et la colonne {@code derniere_erreur} de la base.
     * Une erreur Pastell typique fait quelques dizaines d'octets,
     * mais on protege contre les cas pathologiques.
     */
    private static String truncate(String body) {
        if (body == null) {
            return null;
        }
        if (body.length() <= 1000) {
            return body;
        }
        return body.substring(0, 1000) + "...[tronque]";
    }
}