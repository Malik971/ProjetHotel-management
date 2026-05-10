-- ============================================================
-- V4__pastell_polling_cursor_table.sql
-- Emplacement : sejour-backend/src/main/resources/db/migration/
--
-- Lot 5 : table de curseur pour le polling descendant Pastell -> Sejour.
--
-- Pourquoi une table mono-ligne ?
--
--   Le polling descendant doit retenir UNE seule information : "quel est
--   le dernier id_j (numero d'entree dans le journal Pastell) que j'ai
--   deja traite ?". Cette information est globale au processus, pas par
--   reservation, donc une table mono-ligne suffit.
--
--   On aurait pu :
--     a) Stocker dans un fichier sur disque -> ne survit pas aux redeploiements Render
--     b) Stocker dans Redis -> dependance externe lourde pour une seule valeur
--     c) Coller la colonne dans pastell_sync -> melange granularite globale et
--        granularite par reservation, casse la coherence du modele
--     d) Une table dediee mono-ligne -> persistant, simple, isole. CHOIX RETENU.
--
-- Pourquoi un CHECK (id = 1) ?
--
--   Garantit qu'il ne peut jamais y avoir qu'UNE SEULE ligne dans la table.
--   Sans cette contrainte, un bug pourrait inserer des lignes en double et
--   creer une ambiguite sur "quel curseur lire ?". Avec CHECK + INSERT initial,
--   on a la garantie BDD : il y a toujours exactement 1 ligne, accessible par id=1.
--
-- Pourquoi un INSERT initial dans la migration ?
--
--   La toute premiere fois que le scheduler tourne, il fait SELECT * FROM
--   pastell_polling_cursor WHERE id = 1. Si la ligne n'existe pas, on aurait
--   un cas null a gerer dans le code. En l'inserant ici avec last_processed_id_j=0,
--   on garantit que la ligne existe toujours, et que le code n'a qu'a la lire,
--   la mettre a jour, et la sauver. Plus simple, moins de cas particuliers.
--
--   La valeur initiale 0 signifie : "je n'ai encore rien traite, demande tout
--   au mock Pastell des qu'il a quelque chose". C'est coherent avec le comportement
--   de GET /api/v2/journal?since_id_j=0 cote mock (renvoie tout depuis le debut).
-- ============================================================

CREATE TABLE pastell_polling_cursor (
    -- PK figee a 1 par contrainte CHECK : un seul curseur global possible
                                        id                      BIGINT       PRIMARY KEY,

    -- Dernier id_j du journal Pastell traite avec succes par le polling.
    -- Type BIGINT pour s'aligner sur le type long cote Java et cote mock
    -- (AtomicLong dans MockDocumentStore).
                                        last_processed_id_j     BIGINT       NOT NULL DEFAULT 0,

    -- Horodatage du dernier polling reussi (utile pour observabilite Lot 6)
                                        last_polled_at          TIMESTAMP,

    -- Horodatage de la creation de la ligne (rempli a l'INSERT initial)
                                        date_creation           TIMESTAMP    NOT NULL,

    -- Horodatage de la derniere modification (mis a jour par @PreUpdate JPA)
                                        date_modification       TIMESTAMP    NOT NULL,

    -- Garantit qu'il n'y a jamais qu'une seule ligne dans la table.
    -- Toute tentative d'INSERT avec un id != 1 echoue.
                                        CONSTRAINT pk_pastell_polling_cursor_singleton CHECK (id = 1)
);

-- Insertion de la ligne unique. CURRENT_TIMESTAMP est portable PostgreSQL/H2.
-- last_processed_id_j = 0 signifie "rien encore traite" : au premier poll,
-- le scheduler demandera tout le journal a partir du debut.
INSERT INTO pastell_polling_cursor (
    id,
    last_processed_id_j,
    last_polled_at,
    date_creation,
    date_modification
) VALUES (
             1,
             0,
             NULL,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         );