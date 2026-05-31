-- ============================================================
-- V7 : Passage des hotels vers des images multiples par URL
-- ============================================================
--
-- Avant : hotels.image_url (un seul chemin local, ephemere sur Render)
-- Apres : hotel_images (table de liaison, URLs externes multiples)
--
-- Les anciens hotels voient leur image_url supprimee sans migration
-- de donnees : l'admin les ressaisira via l'interface admin.
-- ============================================================

-- Suppression de l'ancienne colonne image unique
ALTER TABLE hotels DROP COLUMN IF EXISTS image_url;

-- Creation de la table des images multiples
CREATE TABLE IF NOT EXISTS hotel_images (
                                            hotel_id BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
                                            image_url VARCHAR(1000) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_hotel_images_hotel_id ON hotel_images(hotel_id);