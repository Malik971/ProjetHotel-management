-- Migration V9 : champs de signature electronique sur la table reservation.
--
-- Contexte : on introduit un workflow de validation avec signature en deux
-- etapes (niveau 2 = signature locale canvas HTML5, niveau 3 = parapheur
-- distant). La colonne signature_pdf_base64 stocke le PDF recepisse genere
-- apres apposition de la signature. La colonne nom_signataire identifie
-- l'agent qui a signe.
--
-- Les deux nouveaux statuts (SIGNATURE_EN_COURS, SIGNATURE_APPOSEE) sont
-- des chaines STRING dans la colonne VARCHAR existante : Flyway n'a pas a
-- modifier le type de la colonne.
--
-- Taille TEXT : un PDF d'une page encode en base64 occupe environ 50 a 100
-- ko ; TEXT est sans limite pratique sous PostgreSQL.

ALTER TABLE reservation
    ADD COLUMN IF NOT EXISTS signature_pdf_base64 TEXT,
    ADD COLUMN IF NOT EXISTS nom_signataire        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS signed_at             TIMESTAMP;

COMMENT ON COLUMN reservation.signature_pdf_base64 IS
    'PDF recepisse encode en base64, genere apres apposition de la signature. '
        'NULL tant que le dossier n''est pas signe. Au niveau 3, ce PDF sera '
        'retourne par le mock parapheur.';

COMMENT ON COLUMN reservation.nom_signataire IS
    'Nom et prenom de l''agent signataire, tel que saisi au moment de la signature.';

COMMENT ON COLUMN reservation.signed_at IS
    'Horodatage UTC de l''apposition de la signature.';