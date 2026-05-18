-- ============================================================
-- V6 : Table de trace du journal Pastell consomme par Sejour
-- ============================================================
--
-- Cette table sauvegarde localement les entrees du journal Pastell
-- qui ont ete consommees par le polling descendant. Elle ne remplace
-- pas le journal Pastell (Pastell reste la source de verite), mais
-- elle permet a l'admin Sejour de reconstituer rapidement la frise
-- des evenements d'un dossier sans re-interroger Pastell.
--
-- Lien fonctionnel : id_d_pastell joint avec pastell_sync.pastell_document_id
-- pour retrouver la reservation associee a chaque entree.
--
-- Unicite sur id_j : evite les doublons en cas de re-poll du meme
-- since_id_j (peut arriver en cas de crash + redemarrage).
-- ============================================================

CREATE TABLE IF NOT EXISTS pastell_journal_entry_record (
                                                            id                  BIGSERIAL PRIMARY KEY,
                                                            id_j                BIGINT      NOT NULL UNIQUE,
                                                            id_d_pastell        VARCHAR(64) NOT NULL,
                                                            action              VARCHAR(64) NOT NULL,
                                                            id_entite_pastell   BIGINT,
                                                            occurred_at         TIMESTAMP   NOT NULL,
                                                            recorded_at         TIMESTAMP   NOT NULL,
                                                            severity            VARCHAR(16),
                                                            message             VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_journal_doc_id
    ON pastell_journal_entry_record (id_d_pastell);

CREATE INDEX IF NOT EXISTS idx_journal_occurred_at
    ON pastell_journal_entry_record (occurred_at);