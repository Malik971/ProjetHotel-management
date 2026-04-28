package com.example.pastellmock.controller;

import com.example.pastellmock.domain.DocumentTransitions;
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
 * Au Paquet 3, le controller a ete enrichi pour injecter aussi
 * DocumentTransitions, necessaire pour calculer le champ action_possible
 * dans la reponse de detail-document.
 *
 * Le endpoint POST .../{idD}/action (change-action) sera dans un controller
 * separe (ActionController, Vague 3) pour respecter le principe de
 * responsabilite unique : ce controller-ci gere la creation et la lecture,
 * le futur ActionController gerera les transitions d'etat.
 */
@RestController
@RequestMapping("/api/v2/entite/{idEntite}/document")
public class DocumentController {

    private final MockDocumentStore store;
    private final DocumentTransitions transitions;

    public DocumentController(MockDocumentStore store, DocumentTransitions transitions) {
        this.store = store;
        this.transitions = transitions;
    }

    @PostMapping(consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    public ResponseEntity<CreateDocumentResponse> createDocument(
            @PathVariable long idEntite,
            @RequestParam("type") String type) {

        MockDocument created = store.create(idEntite, type);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CreateDocumentResponse.from(created));
    }

    @GetMapping("/{idD}")
    public DetailDocumentResponse getDocument(
            @PathVariable long idEntite,
            @PathVariable String idD) {

        MockDocument doc = store.findById(idD)
                .orElseThrow(() -> new DocumentNotFoundException(idD));

        // Au Paquet 3 : on passe DocumentTransitions pour calculer
        // dynamiquement le champ action_possible
        return DetailDocumentResponse.from(doc, transitions);
    }
}