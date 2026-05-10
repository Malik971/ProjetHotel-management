-- ============================================================
-- V2__pastell_sync_table.sql
-- Emplacement : src/main/resources/db/migration/V2__pastell_sync_table.sql
--
-- Table de synchronisation entre une Reservation locale et son
-- dossier Pastell correspondant.
--
-- Principe architectural (Lot 1) :
--   - Une reservation a au plus UN dossier Pastell (1-1).
--   - Spring est autorite : cette table ne modifie JAMAIS le
--     statut de la reservation. Elle ne fait qu'observer l'etat
--     cote Pastell et signaler les divergences.
--   - En cas d'echec de synchronisation, on n'annule PAS la
--     reservation : on marque sync_status et on laisse un job
--     de reprise (Lot 4) retenter.
-- ============================================================

CREATE TABLE pastell_sync (
                              id                          BIGSERIAL PRIMARY KEY,

    -- Lien vers la reservation locale
    -- UNIQUE : une reservation ne peut etre liee qu'a un seul dossier Pastell
    -- ON DELETE CASCADE : si la reservation est supprimee, son suivi Pastell l'est aussi
                              reservation_id              BIGINT NOT NULL UNIQUE,

    -- Identifiant du dossier Pastell (id_d) retourne par create-document.php
    -- Unique : deux reservations ne peuvent pas pointer le meme dossier Pastell
                              pastell_document_id         VARCHAR(100) NOT NULL UNIQUE,

    -- Dernier etat Pastell connu (observe via polling journal.php, Lot 5)
    -- Exemples : "en-attente-validation", "validee", "confirmee", "annulee"
    -- Peut etre NULL temporairement apres la creation, avant le premier poll
                              pastell_etat_dernier_connu  VARCHAR(100),

    -- Statut technique de la synchronisation cote Sejour
    -- Valeurs : OK, EN_ERREUR, EN_RETRY, DIVERGENCE
    -- DIVERGENCE = Pastell et Sejour ne sont pas d'accord, admin doit arbitrer
                              sync_status                 VARCHAR(20) NOT NULL,

    -- Compteur de tentatives pour le retry (Lot 4)
                              tentatives                  INTEGER NOT NULL DEFAULT 0,

    -- Message d'erreur de la derniere tentative echouee
                              derniere_erreur             TEXT,

    -- Horodatage de la derniere synchronisation reussie (ou tentee)
                              derniere_synchro            TIMESTAMP,

                              date_creation               TIMESTAMP NOT NULL,
                              date_modification           TIMESTAMP NOT NULL,

                              CONSTRAINT fk_pastell_sync_reservation
                                  FOREIGN KEY (reservation_id) REFERENCES reservation(id) ON DELETE CASCADE
);

-- Index pour les requetes du job de reprise (Lot 4)
-- "donne-moi tous les syncs en erreur a retenter"
CREATE INDEX idx_pastell_sync_status ON pastell_sync(sync_status);

-- Index pour la recherche inverse : "a quelle reservation appartient le dossier Pastell XYZ ?"
-- Utile au Lot 5 pour traiter les evenements entrants
CREATE INDEX idx_pastell_sync_document_id ON pastell_sync(pastell_document_id);