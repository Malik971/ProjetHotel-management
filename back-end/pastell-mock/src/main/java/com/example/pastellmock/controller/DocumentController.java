package com.example.pastellmock.controller;

import com.example.pastellmock.domain.MockDocument;
import com.example.pastellmock.dto.CreateDocumentResponse;
import com.example.pastellmock.dto.DetailDocumentResponse;
import com.example.pastellmock.exception.DocumentNotFoundException;
import com.example.pastellmock.store.MockDocumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller HTTP pour les endpoints v2 de gestion des dossiers Pastell.
 *
 * Endpoints exposes :
 *   - POST /api/v2/entite/{idEntite}/document          -> create-document (form-data)
 *   - GET  /api/v2/entite/{idEntite}/document/{idD}    -> detail-document
 *
 * Conventions URL :
 *   - On suit le pattern REST v2 utilise par le client officiel pastell-api-php
 *     de Libriciel : entite est dans l'URL (path), pas en query param.
 *   - L'idEntite est present dans l'URL meme pour le detail, par coherence
 *     avec la doc officielle qui mentionne id_e comme parametre obligatoire
 *     pour toutes les operations sur un document.
 *
 * Mode strict (form-data only) :
 *   - L'attribut consumes du @PostMapping liste les seuls Content-Types acceptes
 *   - Tout autre Content-Type (notamment application/json) declenche
 *     automatiquement un HttpMediaTypeNotSupportedException, traduit en 415
 *     par PastellMockExceptionHandler
 *
 * Securite :
 *   - Ces endpoints tombent sous /api/** dans MockSecurityConfig
 *   - HTTP Basic obligatoire (geree par Spring Security en amont)
 *
 * Architecture :
 *   - Le controller est SANS LOGIQUE METIER : il delegue tout au store
 *   - Sa seule responsabilite : adapter HTTP vers domaine et inversement
 *   - Si une regle metier doit etre ajoutee plus tard (ex: limite par entite),
 *     elle ira dans une couche service entre le controller et le store.
 */
@RestController
@RequestMapping("/api/v2/entite/{idEntite}/document")
public class DocumentController {

    private final MockDocumentStore store;

    /**
     * Injection par constructeur (recommande Spring vs @Autowired sur champ).
     * Avantages :
     *   - Champ final : impossible de muter accidentellement
     *   - Testable sans Spring : on peut faire new DocumentController(mockStore)
     *   - Dependances explicites : on voit immediatement ce dont la classe a besoin
     */
    public DocumentController(MockDocumentStore store) {
        this.store = store;
    }

    /**
     * Cree un nouveau dossier Pastell.
     *
     * Requete attendue :
     * <pre>
     * POST /api/v2/entite/1/document
     * Content-Type: multipart/form-data
     * Authorization: Basic ...
     *
     * type=reservation-hoteliere
     * </pre>
     *
     * Reponse en cas de succes (201 Created) :
     * <pre>
     * {
     *   "id_d": "5f3e8a9b2c1d",
     *   "info": {
     *     "id_d": "5f3e8a9b2c1d",
     *     "id_e": 1,
     *     "type": "reservation-hoteliere",
     *     "last_action": "creation",
     *     "last_action_date": "2026-04-27 16:30:00"
     *   }
     * }
     * </pre>
     *
     * Erreurs possibles (gerees par PastellMockExceptionHandler) :
     *   - 400 Bad Request : champ "type" manquant ou vide, idEntite invalide
     *   - 401 Unauthorized : credentials manquants ou faux (gere par Spring Security)
     *   - 415 Unsupported Media Type : Content-Type pas form-data (mode strict)
     *
     * @param idEntite identifiant numerique de l'entite Pastell (path)
     * @param type     type de dossier a creer (form-data)
     * @return CreateDocumentResponse avec l'id_d genere et les metadonnees
     */
    @PostMapping(consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    public ResponseEntity<CreateDocumentResponse> createDocument(
            @PathVariable long idEntite,
            @RequestParam("type") String type) {

        // Le store gere lui-meme la validation (type non vide, idEntite >= 1)
        // et leve IllegalArgumentException si KO. PastellMockExceptionHandler
        // traduira cette exception en 400 Bad Request automatiquement.
        MockDocument created = store.create(idEntite, type);

        // Reponse 201 Created : convention REST pour une ressource nouvellement
        // creee. Pastell reel renvoie 200 OK, mais 201 est plus correct
        // semantiquement et n'impacte pas le contrat (le client lit le body,
        // pas le code HTTP exact).
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CreateDocumentResponse.from(created));
    }

    /**
     * Recupere les details d'un dossier Pastell existant.
     *
     * Requete attendue :
     * <pre>
     * GET /api/v2/entite/1/document/5f3e8a9b2c1d
     * Authorization: Basic ...
     * </pre>
     *
     * Reponse en cas de succes (200 OK) :
     * <pre>
     * {
     *   "info": { ... },
     *   "data": {},
     *   "action_possible": ["modification"]
     * }
     * </pre>
     *
     * Erreurs possibles :
     *   - 404 Not Found : aucun dossier avec cet id_d dans le store
     *   - 401 Unauthorized : credentials manquants ou faux
     *
     * Note : on n'utilise pas idEntite dans la lookup, car notre store est
     * indexe par id_d uniquement (et un id_d est globalement unique). Le vrai
     * Pastell verifie en plus que le document appartient a l'entite, mais le
     * mock simplifie : si l'id_d existe, on le retourne.
     */
    @GetMapping("/{idD}")
    public DetailDocumentResponse getDocument(
            @PathVariable long idEntite,
            @PathVariable String idD) {

        MockDocument doc = store.findById(idD)
                .orElseThrow(() -> new DocumentNotFoundException(idD));

        return DetailDocumentResponse.from(doc);
    }
}