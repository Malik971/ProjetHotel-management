package com.example.pastellmock.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler global qui traduit les exceptions du mock en reponses HTTP
 * au format Pastell officiel.
 *
 * Format de reponse d'erreur :
 * <pre>
 * {
 *   "status": "error",
 *   "error_message": "..."
 * }
 * </pre>
 *
 * Cas geres (du plus specifique au plus generique) :
 *   - DocumentNotFoundException                  -> 404 (idD inconnu)
 *   - NoResourceFoundException                   -> 404 (route inexistante)
 *   - HttpMediaTypeNotSupportedException         -> 415 (mode strict)
 *   - MissingServletRequestParameterException    -> 400 (champ form-data manquant)
 *   - IllegalStateException                      -> 400 (transition invalide, Paquet 3)
 *   - IllegalArgumentException                   -> 400 (validation metier du store)
 *   - HttpMessageNotReadableException            -> 400 (body illisible)
 *   - Exception (generique)                      -> 500 (filet de securite)
 *
 * Note importante : IllegalStateException et IllegalArgumentException sont
 * tous les deux des RuntimeException, mais avec des semantiques distinctes :
 *   - IllegalArgument : "tu m'as donne un parametre invalide"
 *   - IllegalState    : "ma machine est dans un etat qui ne permet pas
 *                        cette operation"
 * Le store leve l'un ou l'autre selon le cas : type=null -> IllegalArgument,
 * transition impossible -> IllegalState. Les deux meritent un 400 mais avec
 * des messages d'erreur propres a chaque cas.
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
   * Route inexistante -> 404 Not Found.
   * Sans ce handler, le @ExceptionHandler(Exception.class) generique
   * en bas du fichier attraperait NoResourceFoundException et la
   * traduirait en 500.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException ex) {
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorPayload("Route inexistante : " + ex.getResourcePath()));
  }

  /**
   * Mauvais Content-Type sur la requete (typiquement : JSON envoye au lieu
   * de multipart/form-data) -> 415 Unsupported Media Type.
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
   * Transition de workflow invalide (Paquet 3) -> 400 Bad Request.
   *
   * Le store leve IllegalStateException quand une action incompatible avec
   * l'etat courant est demandee, par exemple "confirmation" depuis l'etat
   * "creation". Le message de l'exception est deja parlant ("Action 'X'
   * impossible depuis l'etat 'Y'"), on le retransmet tel quel.
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorPayload(ex.getMessage()));
  }

  /**
   * Validation metier echouee dans le store (ex: type vide, idEntite < 1)
   * -> 400 Bad Request.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorPayload(ex.getMessage()));
  }

  /**
   * Body de requete illisible -> 400 Bad Request.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorPayload("Requete illisible"));
  }

  /**
   * Filet de securite : toute autre exception non geree explicitement -> 500.
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
   */
  private Map<String, String> errorPayload(String message) {
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("status", "error");
    payload.put("error_message", message);
    return payload;
  }
}