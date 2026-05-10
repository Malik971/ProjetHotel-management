package com.example.pastellmock.controller;

import com.example.pastellmock.domain.DocumentTransitions;
import com.example.pastellmock.domain.MockDocument;
import com.example.pastellmock.dto.ChangeActionResponse;
import com.example.pastellmock.exception.DocumentNotFoundException;
import com.example.pastellmock.store.MockDocumentStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller HTTP pour l'action change-action de Pastell.
 *<p>
 * Endpoint expose :
 *   POST /api/v2/entite/{idEntite}/document/{idD}/action
 *<p>
 * Pourquoi un controller separe de DocumentController ?
 *   - SRP : DocumentController gere la creation et la lecture, ActionController
 *     gere les TRANSITIONS d'etat. Deux preoccupations distinctes.
 *   - Le path /action est suffisamment specifique pour merite un controller
 *     dedie sans trop de duplication.
 *   - Au prochain Lot, si on ajoute des endpoints de modification de "data"
 *     (modify-document), ils iront dans un troisieme controller.
 *<p>
 * Mode strict : form-data uniquement (cf. @PostMapping(consumes=...)).
 *<p>
 * Erreurs possibles (toutes traduites par PastellMockExceptionHandler) :
 *   - 400 Bad Request  : champ "action" manquant (MissingServletRequestParameterException)
 *   - 400 Bad Request  : transition invalide (IllegalStateException levee par le store)
 *   - 401 Unauthorized : credentials manquants ou faux (Spring Security)
 *   - 404 Not Found    : idD inconnu (DocumentNotFoundException)
 *   - 415 Unsupported Media Type : Content-Type non form-data
 */
@RestController
@RequestMapping("/api/v2/entite/{idEntite}/document/{idD}/action")
public class ActionController {

    private final MockDocumentStore store;
    private final DocumentTransitions transitions;

    public ActionController(MockDocumentStore store, DocumentTransitions transitions) {
        this.store = store;
        this.transitions = transitions;
    }

    /**
     * Execute une action sur un document existant et applique la transition
     * d'etat correspondante.
     *
     * Requete attendue :
     * <pre>
     * POST /api/v2/entite/1/document/0e2ebd294169/action
     * Content-Type: multipart/form-data
     * Authorization: Basic ...
     *
     * action=validation
     * </pre>
     *
     * Reponse en cas de succes (200 OK) :
     * <pre>
     * {
     *   "result": "ok",
     *   "info": {
     *     "id_d": "0e2ebd294169",
     *     "id_e": 1,
     *     "type": "reservation-hoteliere",
     *     "last_action": "en-attente-validation",
     *     "last_action_date": "2026-04-28 15:30:00"
     *   },
     *   "action_possible": ["annulation", "validation"]
     * }
     * </pre>
     *
     * Note sur le code de retour : 200 OK et non 201 Created, parce qu'on
     * MODIFIE un document existant, on n'en cree pas un nouveau. C'est
     * la convention REST.
     */
    @PostMapping(consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    public ChangeActionResponse executeAction(
            @PathVariable long idEntite,
            @PathVariable String idD,
            @RequestParam("action") String action) {

        // 1) Le document doit exister. Sinon : 404 via DocumentNotFoundException.
        // 2) La transition doit etre valide. Sinon : le store leve
        //    IllegalStateException, traduite en 400 par l'exception handler.
        // Les deux cas d'echec sont geres en amont, on n'a pas a les
        // tester explicitement dans le controller : le code reste minimal.
        MockDocument updated = store.changeAction(idD, action)
                .orElseThrow(() -> new DocumentNotFoundException(idD));

        return ChangeActionResponse.from(updated, transitions);
    }
}