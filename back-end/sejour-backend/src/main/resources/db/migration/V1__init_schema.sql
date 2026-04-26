-- ============================================================
-- V1__init_schema.sql
-- Emplacement : src/main/resources/db/migration/V1__init_schema.sql
-- Schéma initial PostgreSQL pour l'application Séjour
-- ============================================================

-- ───────────────────────────────
-- Table users
-- ───────────────────────────────
CREATE TABLE users (
                       id          BIGSERIAL PRIMARY KEY,
                       first_name  VARCHAR(100),
                       last_name   VARCHAR(100),
                       email       VARCHAR(255) NOT NULL UNIQUE,
                       telephone   VARCHAR(30),
                       password    VARCHAR(255) NOT NULL,
                       enabled     BOOLEAN NOT NULL DEFAULT TRUE
);

-- ───────────────────────────────
-- Table role
-- ───────────────────────────────
CREATE TABLE role (
                      id   BIGSERIAL PRIMARY KEY,
                      name VARCHAR(50) NOT NULL UNIQUE
);

-- ───────────────────────────────
-- Table privilege
-- ───────────────────────────────
CREATE TABLE privilege (
                           id   BIGSERIAL PRIMARY KEY,
                           name VARCHAR(50) NOT NULL UNIQUE
);

-- ───────────────────────────────
-- Table de jointure users_roles
-- ───────────────────────────────
CREATE TABLE users_roles (
                             users_id BIGINT NOT NULL,
                             role_id  BIGINT NOT NULL,
                             PRIMARY KEY (users_id, role_id),
                             CONSTRAINT fk_users_roles_user FOREIGN KEY (users_id) REFERENCES users(id) ON DELETE CASCADE,
                             CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

-- ───────────────────────────────
-- Table de jointure roles_privileges
-- ───────────────────────────────
CREATE TABLE roles_privileges (
                                  role_id      BIGINT NOT NULL,
                                  privilege_id BIGINT NOT NULL,
                                  PRIMARY KEY (role_id, privilege_id),
                                  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_rp_privilege FOREIGN KEY (privilege_id) REFERENCES privilege(id) ON DELETE CASCADE
);

-- ───────────────────────────────
-- Table hotels
-- ───────────────────────────────
CREATE TABLE hotels (
                        id              BIGSERIAL PRIMARY KEY,
                        nom             VARCHAR(255),
                        ville           VARCHAR(255),
                        adresse         VARCHAR(255),
                        description     TEXT,
                        note_moyenne    DOUBLE PRECISION,
                        image_url       VARCHAR(500),
                        date_creation   TIMESTAMP NOT NULL,
                        latitude        DOUBLE PRECISION,
                        longitude       DOUBLE PRECISION,
                        prix_moyen_nuit DOUBLE PRECISION,
                        categorie       INTEGER
);

-- ───────────────────────────────
-- Table hotel_equipements (ElementCollection de Hotel)
-- ───────────────────────────────
CREATE TABLE hotel_equipements (
                                   hotel_id   BIGINT NOT NULL,
                                   equipement VARCHAR(255),
                                   CONSTRAINT fk_hotel_equipements FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE
);

-- ───────────────────────────────
-- Table chambre
-- ───────────────────────────────
CREATE TABLE chambre (
                         id              BIGSERIAL PRIMARY KEY,
                         nom             VARCHAR(255) NOT NULL,
                         prix_par_nuit   NUMERIC(10, 2) NOT NULL,
                         capacity        INTEGER NOT NULL,
                         superficie      INTEGER NOT NULL,
                         type_lit        VARCHAR(255) NOT NULL,
                         description     VARCHAR(1000),
                         hotel_id        BIGINT NOT NULL,
                         date_creation   TIMESTAMP NOT NULL,
                         CONSTRAINT fk_chambre_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE
);

-- ───────────────────────────────
-- Table chambre_equipements (ElementCollection de Chambre)
-- ───────────────────────────────
CREATE TABLE chambre_equipements (
                                     chambre_id BIGINT NOT NULL,
                                     equipement VARCHAR(255),
                                     CONSTRAINT fk_chambre_equipements FOREIGN KEY (chambre_id) REFERENCES chambre(id) ON DELETE CASCADE
);

-- ───────────────────────────────
-- Table chambre_images (ElementCollection de Chambre)
-- ───────────────────────────────
CREATE TABLE chambre_images (
                                chambre_id BIGINT NOT NULL,
                                image_url  VARCHAR(500),
                                CONSTRAINT fk_chambre_images FOREIGN KEY (chambre_id) REFERENCES chambre(id) ON DELETE CASCADE
);

-- ───────────────────────────────
-- Table reservation
-- ───────────────────────────────
CREATE TABLE reservation (
                             id                 BIGSERIAL PRIMARY KEY,
                             date_debut         DATE NOT NULL,
                             date_fin           DATE NOT NULL,
                             nom_client         VARCHAR(255) NOT NULL,
                             email_client       VARCHAR(255) NOT NULL,
                             telephone_client   VARCHAR(255) NOT NULL,
                             nombre_personnes   INTEGER NOT NULL,
                             prix_total         DOUBLE PRECISION NOT NULL,
                             statut             VARCHAR(50) NOT NULL,
                             code_confirmation  VARCHAR(255) UNIQUE,
                             chambre_id         BIGINT NOT NULL,
                             user_id            BIGINT,
                             date_creation      TIMESTAMP NOT NULL,
                             CONSTRAINT fk_reservation_chambre FOREIGN KEY (chambre_id) REFERENCES chambre(id),
                             CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ───────────────────────────────
-- Index utiles pour les performances
-- ───────────────────────────────
CREATE INDEX idx_chambre_hotel_id ON chambre(hotel_id);
CREATE INDEX idx_reservation_chambre_id ON reservation(chambre_id);
CREATE INDEX idx_reservation_user_id ON reservation(user_id);
CREATE INDEX idx_reservation_dates ON reservation(date_debut, date_fin);
CREATE INDEX idx_users_email ON users(email);