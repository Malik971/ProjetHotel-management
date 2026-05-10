-- ============================================================
-- V3__pastell_sync_make_doc_id_nullable.sql
-- Emplacement : sejour-backend/src/main/resources/db/migration/
--
-- Lot 3 : rend la colonne pastell_document_id nullable.
--
-- Pourquoi ?
--   Au Lot 1, la colonne etait NOT NULL parce qu'on supposait
--   qu'un PastellSync n'existerait qu'apres un appel reussi a
--   create-document.php (donc avec un id_d garanti).
--
--   Au Lot 3, on introduit le statut PENDING : un PastellSync
--   est persiste DES la creation de la reservation, AVANT le
--   premier appel HTTP. Si cet appel echoue (timeout, 5xx,
--   crash serveur), on doit pouvoir stocker le sync sans id_d
--   pour que le job de reprise (Lot 4) le retrouve et retente.
--
--   La contrainte UNIQUE est conservee : PostgreSQL accepte
--   plusieurs NULL dans une colonne UNIQUE (norme SQL standard).
--   Deux reservations ne pourront jamais pointer le meme id_d,
--   mais plusieurs syncs PENDING peuvent coexister avec id_d = NULL.
-- ============================================================

ALTER TABLE pastell_sync
    ALTER COLUMN pastell_document_id DROP NOT NULL;