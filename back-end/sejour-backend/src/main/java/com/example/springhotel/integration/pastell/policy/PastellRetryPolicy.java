package com.example.springhotel.integration.pastell.policy;

import com.example.springhotel.integration.pastell.client.PastellApiException;
import org.springframework.stereotype.Component;

/**
 * Politique de decision retryable / non-retryable pour les appels Pastell.
 *<p>
 * Principe : un appel echoue, on doit decider si ca vaut le coup de re-essayer.
 *<p>
 *   - Erreur reseau (timeout, connexion refusee, DNS) : Pastell est peut-etre
 *     juste momentanement injoignable. RETRYABLE.
 *<p>
 *   - HTTP 5xx (Internal Server Error, Bad Gateway, Service Unavailable) :
 *     panne cote Pastell, peut se resoudre seule. RETRYABLE.
 *<p>
 *   - HTTP 408 Request Timeout : Pastell a recu la requete mais a timeout
 *     en interne. RETRYABLE.
 *<p>
 *   - HTTP 429 Too Many Requests : on tape trop fort, Pastell rate-limite.
 *     RETRYABLE (avec un backoff plus long en pratique).
 *<p>
 *   - HTTP 4xx (sauf 408/429) : la requete elle-meme est invalide. Re-essayer
 *     ne changera rien. NON-RETRYABLE :
 *       - 400 : payload mal forme (bug cote Sejour)
 *       - 401 : credentials invalides (mauvaise config)
 *       - 403 : pas le droit (entite mal configuree)
 *       - 404 : ressource introuvable (id_d perimee)
 *       - 409 : conflit metier (etat inattendu)
 *<p>
 *   - HTTP 2xx avec body anormal (ex. id_d manquant) : on a deja eu une
 *     reponse "succes" mais elle est inexploitable. Re-essayer ne va pas
 *     magiquement faire apparaitre un id_d. NON-RETRYABLE.
 *<p>
 * Pourquoi un composant Spring et pas une enum statique ?
 *   - Pour pouvoir l'injecter dans le wrapper de retry et le scheduler
 *     (les deux ont besoin de cette logique).
 *   - Pour pouvoir le mocker dans les tests si on voulait simuler des
 *     politiques alternatives.
 *   - L'instance est sans etat, donc le scope singleton de Spring est neutre.
 *<p>
 * Pourquoi pas de @ConditionalOnProperty ?
 *   - Cette classe est utilitaire, sans dependance metier ni HTTP. Meme si
 *     pastell.enabled=false, l'instancier est gratuit et innofensif. Pas la
 *     peine de la conditionner.
 */
@Component
public class PastellRetryPolicy {

    /**
     * Decide si une exception Pastell merite un retry.
     *
     * @param e exception remontee par PastellClient
     * @return true si on doit re-essayer, false sinon
     */
    public boolean isRetryable(PastellApiException e) {
        if (e == null) {
            return false;
        }
        // Erreur reseau (statusCode = -1) : on a meme pas eu de reponse, c'est du transport.
        if (!e.hasHttpResponse()) {
            return true;
        }
        int status = e.getStatusCode();

        // 2xx avec body cassee : non-retryable, le serveur a repondu OK.
        if (status >= 200 && status < 300) {
            return false;
        }
        // Cas particuliers 4xx retryables.
        if (status == 408 || status == 429) {
            return true;
        }
        // Reste des 4xx : non-retryable.
        if (status >= 400 && status < 500) {
            return false;
        }
        // 5xx : retryable.
        if (status >= 500 && status < 600) {
            return true;
        }
        // Cas exotique (status code hors plage HTTP standard) : on ne retry pas
        // par prudence, ca evite une boucle infinie sur un comportement bizarre.
        return false;
    }

    /**
     * Decide si un message d'erreur deja persiste dans
     * {@code PastellSync.derniereErreur} merite un retry.
     *
     * Format attendu (genere par PastellSyncService.buildErrorMessage) :
     *   - "[401] message" pour une erreur HTTP 401
     *   - "[NETWORK] message" pour une erreur reseau
     *
     * Cette methode permet au scheduler de filtrer les syncs en EN_RETRY
     * sans avoir a re-creer une exception.
     *
     * @param derniereErreur contenu de la colonne derniere_erreur
     * @return true si on doit re-essayer, false sinon (ou si format inconnu)
     */
    public boolean isRetryable(String derniereErreur) {
        if (derniereErreur == null || derniereErreur.isBlank()) {
            // Pas d'info : on retry, charge au scheduler de logger
            // (une absence d'erreur sur un EN_RETRY est anormale mais
            // pas une raison de declarer le sync mort).
            return true;
        }
        // Cas reseau : prefixe [NETWORK]
        if (derniereErreur.startsWith("[NETWORK]")) {
            return true;
        }
        // Cas HTTP : extraire le code du prefixe [xxx]
        if (derniereErreur.startsWith("[")) {
            int end = derniereErreur.indexOf(']');
            if (end > 1) {
                String codeStr = derniereErreur.substring(1, end);
                try {
                    int code = Integer.parseInt(codeStr);
                    return isRetryableHttpStatus(code);
                } catch (NumberFormatException nfe) {
                    // Format inattendu, on prefere ne pas retry
                    return false;
                }
            }
        }
        // Format inconnu : on prefere ne pas retry pour eviter de spammer
        return false;
    }

    private boolean isRetryableHttpStatus(int status) {
        if (status == 408 || status == 429) return true;
        if (status >= 400 && status < 500) return false;
        if (status >= 500 && status < 600) return true;
        return false;
    }
}