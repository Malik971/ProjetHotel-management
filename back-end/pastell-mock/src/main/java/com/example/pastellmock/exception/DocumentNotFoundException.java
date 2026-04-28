package com.example.pastellmock.exception;

/**
 * Exception levee quand un document Pastell n'est pas trouve dans le store.
 *
 * Pourquoi une classe dediee ?
 *   - Permet a {@link PastellMockExceptionHandler} de la cibler explicitement
 *     pour la traduire en HTTP 404 avec un message structure
 *   - Distingue un "document introuvable" (qui est une erreur metier normale,
 *     attendue en cas de mauvais idD cote client) d'une erreur technique
 *     (NullPointerException, etc.) qui doit produire un 500.
 *
 * Pourquoi RuntimeException et non Exception ?
 *   - Pas besoin de la declarer dans les signatures (throws...)
 *   - Convention Spring : les exceptions levees par les controllers sont
 *     unchecked, et c'est le ControllerAdvice qui les attrape.
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String idD) {
        super("Document introuvable : " + idD);
    }
}