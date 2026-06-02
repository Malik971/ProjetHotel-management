-- ============================================================
-- V8 : Ajout de la colonne keycloak_sub sur la table users
-- ============================================================
--
-- Contexte Lot K3 : JIT provisioning Keycloak.
-- Quand un utilisateur se connecte pour la premiere fois via
-- Keycloak (Authorization Code PKCE), le backend cree un profil
-- Users en base et y associe le claim "sub" de son token Keycloak.
-- Ce sub est un UUID stable emis par Keycloak (ex: d9bc9f69-ab0b-...).
--
-- Pourquoi nullable :
--   Les comptes existants (JWT maison, test@test.com, demo@springhotel.fr)
--   n'ont pas de sub Keycloak. Cette colonne ne les concerne pas.
--   On ne peut pas la mettre NOT NULL sans fournir une valeur par defaut
--   pour toutes les lignes existantes, ce qui n'a pas de sens ici.
--
-- Pourquoi unique :
--   Un sub Keycloak identifie un utilisateur de maniere unique dans
--   le realm. Deux profils ne peuvent pas partager le meme sub.
--   La contrainte est sur les valeurs non nulles uniquement
--   (PostgreSQL ignore les NULL dans les contraintes UNIQUE).
--
-- Impact sur les flux existants : aucun.
--   Les colonnes email et password restent inchangees.
--   Le flux JWT maison (LoginController) ne lit pas keycloak_sub.
-- ============================================================

ALTER TABLE users
    ADD COLUMN keycloak_sub VARCHAR(255);

ALTER TABLE users
    ADD CONSTRAINT uq_users_keycloak_sub UNIQUE (keycloak_sub);

CREATE INDEX idx_users_keycloak_sub ON users (keycloak_sub)
    WHERE keycloak_sub IS NOT NULL;