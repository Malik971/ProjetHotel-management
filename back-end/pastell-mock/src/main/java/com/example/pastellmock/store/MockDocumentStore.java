package com.example.pastellmock.store;

import com.example.pastellmock.domain.MockDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store en memoire thread-safe pour les dossiers Pastell.
 *
 * Role :
 *   - Stocker les MockDocument crees via POST /api/v2/entite/{id}/document
 *   - Permettre leur lecture via GET /api/v2/entite/{id}/document/{idD}
 *   - Plus tard (Paquet 3) : permettre leur mutation (change-action)
 *
 * Pourquoi un Component Spring ?
 *   - Pour pouvoir l'injecter dans le DocumentController via @Autowired
 *   - Singleton par defaut : une seule instance partagee dans toute l'appli
 *   - Coherent avec ConcurrentHashMap : un seul classeur, accessible
 *     simultanement par toutes les requetes HTTP
 *
 * Pourquoi ConcurrentHashMap ?
 *   - Plusieurs threads HTTP peuvent appeler create/get/delete en parallele
 *   - Une HashMap classique serait corrompue en cas d'acces concurrent
 *   - ConcurrentHashMap garantit la coherence sans verrous explicites
 *   - Performance : verrous fins par segment, pas global
 *
 * Initialisation :
 *   - Le store demarre VIDE a chaque demarrage de l'application
 *   - Pas de pre-chargement de donnees, pas de persistance
 *   - C'est volontaire : un mock doit etre reproductible, sans etat parasite
 */
@Component
public class MockDocumentStore {

    /**
     * Le "classeur" : cle = idD, valeur = le document complet.
     */
    private final ConcurrentHashMap<String, MockDocument> documents = new ConcurrentHashMap<>();

    /**
     * Cree un nouveau document et le stocke.
     *
     * Genere un idD unique (UUID tronque a 12 caracteres) et fixe :
     *   - lastAction = "creation"
     *   - lastActionDate = maintenant
     *
     * Pourquoi tronquer l'UUID ?
     *   - UUID complet = 36 caracteres, peu lisible
     *   - 12 hex chars = 16 puissance 12 = 281 000 milliards de valeurs possibles
     *   - Risque de collision en pratique : zero pour un mock
     *
     * @param idEntite identifiant numerique de l'entite (>= 1)
     * @param type     type de dossier (non vide)
     * @return le MockDocument cree, avec son idD genere
     * @throws IllegalArgumentException si type est null/vide ou idEntite < 1
     */
    public MockDocument create(long idEntite, String type) {
        if (idEntite < 1) {
            throw new IllegalArgumentException("idEntite doit etre >= 1, recu: " + idEntite);
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type ne peut pas etre vide");
        }

        String idD = generateIdD();
        MockDocument doc = new MockDocument();
        doc.setIdD(idD);
        doc.setType(type);
        doc.setIdEntite(idEntite);
        doc.setLastAction("creation");
        doc.setLastActionDate(LocalDateTime.now());

        documents.put(idD, doc);
        return doc;
    }

    /**
     * Recupere un document par son idD.
     *
     * Retourne un Optional plutot que de lever une exception. Pourquoi ?
     *   - Le store est une couche bas niveau : il ne sait pas si "absent"
     *     est une erreur ou pas. C'est le controller (couche au-dessus) qui
     *     decide si l'absence d'un document doit produire un 404.
     *   - Optional rend l'API explicite : le caller doit gerer le cas absent.
     *
     * @param idD identifiant unique du document
     * @return Optional contenant le document si trouve, vide sinon
     */
    public Optional<MockDocument> findById(String idD) {
        if (idD == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(documents.get(idD));
    }

    /**
     * Verifie si un document existe.
     * Utile pour des checks rapides sans charger l'objet.
     */
    public boolean exists(String idD) {
        return idD != null && documents.containsKey(idD);
    }

    /**
     * Vide le store. Reserve aux tests : permet de garantir un etat propre
     * entre chaque scenario sans avoir a redemarrer le contexte Spring.
     *
     * Pas appele par le code de production : aucun endpoint HTTP n'expose
     * cette operation (ce serait dangereux en cas de fuite vers une vraie
     * instance Pastell).
     */
    public void clear() {
        documents.clear();
    }

    /**
     * Compte le nombre de documents stockes. Pratique pour les assertions
     * de test ("apres N creations, le store contient N documents").
     */
    public int size() {
        return documents.size();
    }

    /**
     * Genere un idD unique : UUID tronque a 12 caracteres hex.
     * Package-private pour permettre des tests deterministes
     * eventuels via heritage si besoin un jour.
     */
    String generateIdD() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}