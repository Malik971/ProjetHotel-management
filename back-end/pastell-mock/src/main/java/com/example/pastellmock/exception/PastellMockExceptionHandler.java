package com.example.pastellmock.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler global qui traduit les exceptions du mock en reponses HTTP
 * au format Pastell officiel.
 *
 * Format de reponse d'erreur (cf. doc Pastell officielle) :
 * <pre>
 * {
 *   "status": "error",
 *   "error_message": "..."
 * }
 * </pre>
 *
 * Pourquoi @RestControllerAdvice plutot que des try/catch dans le controller ?
 *   - DRY : un seul endroit pour formatter toutes les erreurs
 *   - Lisibilite : le code metier du controller reste focalise sur le
 *     happy path, pas pollue par des try/catch
 *   - Coherence : impossible d'oublier de gerer un cas d'erreur quelque part
 *
 * Cas geres :
 *   - DocumentNotFoundException     -> 404
 *   - HttpMediaTypeNotSupportedException (ex: JSON envoye au lieu de form-data) -> 415
 *   - MissingServletRequestParameterException (champ form-data manquant) -> 400
 *   - IllegalArgumentException (validation metier du store) -> 400
 *   - HttpMessageNotReadableException (body illisible) -> 400
 *   - Exception generique (filet de securite) -> 500
 *
 * Note : on n'attrape pas les exceptions Spring Security (401, 403). Spring
 * Security a son propre mecanisme de reponse, anterieur au DispatcherServlet,
 * et qu'on a deja vu fonctionner au Paquet 1 (les 401 que tu as testes en curl).
 */
@RestControllerAdvice
public class PastellMockExceptionHandler {

  /**
   * Document introuvable -> 404 Not Found
   */
  @ExceptionHandler(DocumentNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(DocumentNotFoundException ex) {
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorPayload(ex.getMessage()));
  }

  /**
   * Mauvais Content-Type sur la requete (typiquement : JSON envoye au lieu
   * de multipart/form-data) -> 415 Unsupported Media Type.
   *
   * C'est cette exception qui realise le "mode strict" : Spring la leve
   * automatiquement quand le client envoie un Content-Type non liste dans
   * l'attribut consumes du @PostMapping.
   */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<Map<String, String>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
    String message = "Content-Type non supporte. Attendu : multipart/form-data ou application/x-www-form-urlencoded";
    return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(errorPayload(message));
  }

  /**
   * Champ form-data obligatoire manquant -> 400 Bad Request.
   * Ex: POST sur /document sans le champ "type".
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
    String message = "Parametre obligatoire manquant : " + ex.getParameterName();
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorPayload(message));
  }

  /**
   * Validation metier echouee dans le store (ex: type vide, idEntite < 1)
   * -> 400 Bad Request.
   * Le store leve IllegalArgumentException, on la traduit ici proprement.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorPayload(ex.getMessage()));
  }

  /**
   * Body de requete illisible (rare avec form-data, mais possible si
   * quelqu'un envoie un body malforme) -> 400 Bad Request.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorPayload("Requete illisible"));
  }

  /**
   * Route inexistante -> 404 Not Found.
   *
   * Sans ce handler, le @ExceptionHandler(Exception.class) generique
   * en bas du fichier attraperait NoResourceFoundException et la
   * traduirait en 500, ce qui est faux : une URL inconnue est un 404,
   * pas une erreur serveur.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException ex) {
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorPayload("Route inexistante : " + ex.getResourcePath()));
  }

  /**
   * Filet de securite : toute autre exception non geree explicitement -> 500.
   *
   * En production reelle on ne mettrait JAMAIS le message de l'exception
   * dans la reponse (fuite d'information). Mais c'est un mock dev/CI,
   * exposer le detail aide au debug.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
    String message = "Erreur interne du mock : " + ex.getClass().getSimpleName() + " - " + ex.getMessage();
    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorPayload(message));
  }

  /**
   * Construit le payload d'erreur au format Pastell officiel.
   * LinkedHashMap pour preserver l'ordre des cles (status avant error_message).
   */
  private Map<String, String> errorPayload(String message) {
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("status", "error");
    payload.put("error_message", message);
    return payload;
  }
}