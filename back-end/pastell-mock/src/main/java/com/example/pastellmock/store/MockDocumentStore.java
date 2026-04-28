package com.example.pastellmock.store;

import com.example.pastellmock.domain.DocumentTransitions;
import com.example.pastellmock.domain.JournalEntry;
import com.example.pastellmock.domain.MockDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Store en memoire thread-safe pour les dossiers Pastell + leur journal.
 *
 * Au Paquet 3, le store ne fait plus que stocker des documents : il maintient
 * aussi un JOURNAL chronologique de tous les changements d'etat (creation et
 * change-action). Ce journal sera lu via GET /api/v2/journal pour le polling
 * descendant Pastell -> sejour-backend (Lot 5).
 *
 * Responsabilites :
 *   - Stocker / retrouver les MockDocument
 *   - Faire evoluer un document (changeAction) en validant la transition via
 *     DocumentTransitions
 *   - Maintenir le journal en synchronisation avec les changements d'etat
 *
 * Garanties de coherence :
 *   - Une operation reussie sur un document implique TOUJOURS une entree de
 *     journal correspondante (impossible d'avoir un document modifie sans
 *     trace dans le journal)
 *   - Une operation echouee (transition invalide) ne modifie NI le document
 *     NI le journal
 *
 * Concurrence :
 *   - documents     : ConcurrentHashMap (lectures/ecritures concurrentes safe)
 *   - journal       : CopyOnWriteArrayList (optimal pour beaucoup de lectures
 *                     et peu d'ecritures, ce qui est le cas du polling)
 *   - journalIdSeq  : AtomicLong (compteur monotone thread-safe)
 *
 * Limites assumees (mock dev/CI) :
 *   - Pas de persistance : redemarrage = oubli total
 *   - Pas de limite de taille : un test stress prolonge ferait grossir
 *     indefiniment le journal. Acceptable pour un mock.
 */
@Component
public class MockDocumentStore {

    private final DocumentTransitions transitions;

    /**
     * Table principale : id_d -> document complet.
     */
    private final ConcurrentHashMap<String, MockDocument> documents = new ConcurrentHashMap<>();

    /**
     * Journal chronologique. CopyOnWriteArrayList parce que :
     *   - Le polling lit le journal frequemment (toutes les 30s cote sejour-backend)
     *   - Les ecritures sont peu frequentes (1 par creation/change-action)
     *   - CopyOnWriteArrayList est concue pour ce ratio : lectures sans verrou
     */
    private final List<JournalEntry> journal = new CopyOnWriteArrayList<>();

    /**
     * Compteur monotone pour generer les id_j du journal.
     * AtomicLong garantit l'increment atomique, meme sous concurrence.
     * Demarre a 0 : le premier id_j genere sera 1 (incrementAndGet).
     */
    private final AtomicLong journalIdSeq = new AtomicLong(0);

    public MockDocumentStore(DocumentTransitions transitions) {
        this.transitions = transitions;
    }

    // ============================================================
    // CREATE (modifie au Paquet 3 : ajoute aussi une entree de journal)
    // ============================================================

    /**
     * Cree un nouveau document et ajoute une entree "creation" au journal.
     *
     * IMPORTANT : la creation du document et l'ajout au journal sont effectues
     * dans cet ordre. En cas d'echec d'allocation memoire entre les deux, on
     * pourrait avoir un document sans entree de journal. C'est theoriquement
     * possible mais en pratique rarissime, et acceptable pour un mock.
     * En production reelle, on utiliserait une transaction.
     *
     * @throws IllegalArgumentException si idEntite < 1 ou type vide
     */
    public MockDocument create(long idEntite, String type) {
        if (idEntite < 1) {
            throw new IllegalArgumentException("idEntite doit etre >= 1, recu: " + idEntite);
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type ne peut pas etre vide");
        }

        String idD = generateIdD();
        LocalDateTime now = LocalDateTime.now();

        MockDocument doc = new MockDocument();
        doc.setIdD(idD);
        doc.setType(type);
        doc.setIdEntite(idEntite);
        doc.setLastAction(DocumentTransitions.CREATION);
        doc.setLastActionDate(now);

        documents.put(idD, doc);

        // Tracage dans le journal : "creation" est un etat, pas une action
        appendJournal(idD, idEntite, DocumentTransitions.CREATION, now);

        return doc;
    }

    // ============================================================
    // CHANGE ACTION (nouveau au Paquet 3)
    // ============================================================

    /**
     * Applique une action sur un document et met a jour son etat.
     *
     * Sequence atomique (du point de vue du caller) :
     *   1. Le document doit exister (sinon Optional.empty)
     *   2. La transition (etat_courant, action) doit etre valide
     *      (sinon IllegalStateException)
     *   3. Si OK : le document est mute (lastAction + lastActionDate)
     *   4. Une entree de journal est ajoutee
     *
     * Pourquoi Optional<MockDocument> en retour pour "document inconnu" mais
     * IllegalStateException pour "transition invalide" ?
     *   - "document inconnu" est un cas attendu cote client (mauvais id_d) :
     *     se traduira en HTTP 404, donc on retourne empty pour laisser le
     *     controller decider.
     *   - "transition invalide" est une violation de contrat : le client a
     *     envoye une action incompatible avec l'etat courant. Se traduira
     *     en HTTP 400 via le exception handler. On leve une exception parce
     *     que c'est une erreur, pas un cas attendu de fonctionnement normal.
     *
     * @param idD    identifiant du document
     * @param action action declenchante (ex: "validation")
     * @return Optional avec le document mis a jour, vide si idD inconnu
     * @throws IllegalStateException si la transition n'est pas valide
     */
    public Optional<MockDocument> changeAction(String idD, String action) {
        MockDocument doc = documents.get(idD);
        if (doc == null) {
            return Optional.empty();
        }

        String currentState = doc.getLastAction();
        Optional<String> targetState = transitions.resolveTargetState(currentState, action);

        if (targetState.isEmpty()) {
            throw new IllegalStateException(
                    "Action '" + action + "' impossible depuis l'etat '" + currentState + "'"
            );
        }

        // Mutation du document. Sous concurrence, deux changeAction simultanes
        // sur le meme document pourraient se chevaucher : on accepte ce risque
        // pour un mock. En production reelle, on utiliserait un lock par document
        // ou une transaction optimiste sur un champ version.
        LocalDateTime now = LocalDateTime.now();
        doc.setLastAction(targetState.get());
        doc.setLastActionDate(now);

        appendJournal(doc.getIdD(), doc.getIdEntite(), targetState.get(), now);

        return Optional.of(doc);
    }

    // ============================================================
    // FIND
    // ============================================================

    public Optional<MockDocument> findById(String idD) {
        if (idD == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(documents.get(idD));
    }

    public boolean exists(String idD) {
        return idD != null && documents.containsKey(idD);
    }

    public int size() {
        return documents.size();
    }

    // ============================================================
    // JOURNAL (nouveau au Paquet 3)
    // ============================================================

    /**
     * Retourne les entrees du journal dont l'idJ est strictement superieur a
     * la borne fournie, dans l'ordre chronologique (du plus ancien au plus recent).
     *
     * @param sinceIdJ borne exclusive (passer 0 pour tout recuperer depuis le debut)
     * @return liste immuable des entrees, vide si aucune
     */
    public List<JournalEntry> findJournalEntriesAfter(long sinceIdJ) {
        List<JournalEntry> result = new ArrayList<>();
        for (JournalEntry entry : journal) {
            if (entry.getIdJ() > sinceIdJ) {
                result.add(entry);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Retourne toutes les entrees du journal dans l'ordre chronologique.
     */
    public List<JournalEntry> findAllJournalEntries() {
        return Collections.unmodifiableList(new ArrayList<>(journal));
    }

    /**
     * Taille courante du journal.
     */
    public int journalSize() {
        return journal.size();
    }

    /**
     * Methode interne qui cree et ajoute une entree au journal.
     */
    private void appendJournal(String idD, long idEntite, String action, LocalDateTime date) {
        JournalEntry entry = new JournalEntry(
                journalIdSeq.incrementAndGet(),
                idD,
                idEntite,
                action,
                date
        );
        journal.add(entry);
    }

    // ============================================================
    // TEST UTILITIES
    // ============================================================

    /**
     * Vide le store (documents + journal + sequence). Reserve aux tests.
     */
    public void clear() {
        documents.clear();
        journal.clear();
        journalIdSeq.set(0);
    }

    /**
     * Generation de l'idD : UUID tronque a 12 caracteres hex.
     */
    String generateIdD() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}