package com.example.pastellmock.controller;

import com.example.pastellmock.domain.JournalEntry;
import com.example.pastellmock.dto.JournalEntryResponse;
import com.example.pastellmock.store.MockDocumentStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller HTTP pour le journal Pastell.
 *
 * Endpoint expose :
 *   GET /api/v2/journal[?since_id_j=N]
 *
 * Pourquoi pas sous /entite/{idEntite}/ ?
 *   - Le journal est GLOBAL au mock : il trace toutes les transitions de
 *     tous les documents, peu importe l'entite.
 *   - Pastell reel filtre cote serveur, mais le mock simplifie : on retourne
 *     tout, le client filtre cote lui s'il en a besoin (le champ id_e est
 *     present dans chaque entree).
 *
 * Pourquoi un parametre since_id_j et pas since_date ?
 *   - L'idJ est strictement croissant, deux dates peuvent collisionner a la
 *     seconde voire la milliseconde. Filtrer par idJ est non ambigu.
 *   - C'est exactement ce que sejour-backend fera au Lot 5 : retenir le
 *     dernier idJ vu, demander "tout ce qui est plus recent".
 *
 * Securite : sous /api/**, donc HTTP Basic obligatoire.
 *
 * Note sur la performance : findJournalEntriesAfter() parcourt linearement
 * la liste. Pour un mock en CI, parfait. Pour de la prod, on indexerait.
 */
@RestController
@RequestMapping("/api/v2/journal")
public class JournalController {

    private final MockDocumentStore store;

    public JournalController(MockDocumentStore store) {
        this.store = store;
    }

    /**
     * Retourne les entrees du journal, optionnellement filtrees par idJ.
     *
     * Requete attendue :
     * <pre>
     * GET /api/v2/journal?since_id_j=42
     * Authorization: Basic ...
     * </pre>
     *
     * Reponse (200 OK) :
     * <pre>
     * [
     *   {"id_j": 43, "id_d": "...", "id_e": 1, "action": "validee", "date": "..."},
     *   {"id_j": 44, "id_d": "...", "id_e": 1, "action": "confirmee", "date": "..."}
     * ]
     * </pre>
     *
     * Si aucune entree, la reponse est un tableau vide [], pas 404.
     * "Pas de resultat" est un cas valide, pas une erreur.
     *
     * @param sinceIdJ borne exclusive : seules les entrees dont idJ > sinceIdJ
     *                 sont retournees. Si absent, retourne tout.
     */
    @GetMapping
    public List<JournalEntryResponse> getJournal(
            @RequestParam(name = "since_id_j", required = false, defaultValue = "0")
            long sinceIdJ) {

        List<JournalEntry> entries = store.findJournalEntriesAfter(sinceIdJ);

        return entries.stream()
                .map(JournalEntryResponse::from)
                .toList();
    }
}