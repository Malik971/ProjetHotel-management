-- ============================================================
-- V5__seed_demo_hotels.sql
-- Emplacement : src/main/resources/db/migration/V5__seed_demo_hotels.sql
-- ------------------------------------------------------------
-- Seed des hotels et chambres pour la demo publique (Lot 6).
--
-- Hotels fictifs mais inspires des quartiers reels de Montpellier
-- (Antigone, Comedie, Ecusson, Port Marianne, Pres d'Arenes).
-- Coordonnees GPS realistes pour que la carte react-leaflet soit
-- lisible et que le clustering visuel ait du sens.
--
-- IMPORTANT : ce script utilise INSERT INTO ... SELECT WHERE NOT EXISTS
-- pour rester IDEMPOTENT. Re-executer Flyway (ou redeployer) ne dupliquera
-- pas les donnees. Pour repartir from-scratch, faire DROP/CREATE de la base
-- avant Flyway.
-- ============================================================

-- ───────────────────────────────
-- 5 hotels Montpellier
-- ───────────────────────────────

INSERT INTO hotels (nom, ville, adresse, description, note_moyenne, image_url,
                    date_creation, latitude, longitude, prix_moyen_nuit, categorie)
SELECT 'Le Peyrou', 'Montpellier', '12 place du Peyrou',
       'Hotel de charme face a la Promenade du Peyrou. Vue sur l''Arc de Triomphe et acces direct au centre historique.',
       4.6, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800',
       CURRENT_TIMESTAMP, 43.6112, 3.8703, 145.0, 4
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE nom = 'Le Peyrou');

INSERT INTO hotels (nom, ville, adresse, description, note_moyenne, image_url,
                    date_creation, latitude, longitude, prix_moyen_nuit, categorie)
SELECT 'La Comedie', 'Montpellier', '3 place de la Comedie',
       'Au coeur de la place mythique de Montpellier. Reveils en terrasse face a la fontaine des Trois Graces.',
       4.4, 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800',
       CURRENT_TIMESTAMP, 43.6086, 3.8794, 130.0, 4
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE nom = 'La Comedie');

INSERT INTO hotels (nom, ville, adresse, description, note_moyenne, image_url,
                    date_creation, latitude, longitude, prix_moyen_nuit, categorie)
SELECT 'Antigone Plaza', 'Montpellier', '45 place du Nombre d''Or',
       'Architecture neoclassique de Ricardo Bofill, piscine sur le toit, vue sur le Lez.',
       4.7, 'https://images.unsplash.com/photo-1455587734955-081b22074882?w=800',
       CURRENT_TIMESTAMP, 43.6065, 3.8902, 175.0, 5
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE nom = 'Antigone Plaza');

INSERT INTO hotels (nom, ville, adresse, description, note_moyenne, image_url,
                    date_creation, latitude, longitude, prix_moyen_nuit, categorie)
SELECT 'Port Marianne', 'Montpellier', '88 avenue Raymond Dugrand',
       'Bord du Lez, terrasses, design contemporain. A 10 min en tram du centre.',
       4.3, 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800',
       CURRENT_TIMESTAMP, 43.5990, 3.9070, 110.0, 3
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE nom = 'Port Marianne');

INSERT INTO hotels (nom, ville, adresse, description, note_moyenne, image_url,
                    date_creation, latitude, longitude, prix_moyen_nuit, categorie)
SELECT 'Les Pres d''Arenes', 'Montpellier', '22 rue de l''Aiguelongue',
       'Hotel familial dans un quartier residentiel calme. Bon rapport qualite-prix pour les sejours longs.',
       4.0, 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=800',
       CURRENT_TIMESTAMP, 43.5912, 3.8721, 78.0, 2
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE nom = 'Les Pres d''Arenes');

-- ───────────────────────────────
-- Equipements par hotel
-- ───────────────────────────────

INSERT INTO hotel_equipements (hotel_id, equipement)
SELECT h.id, e.equipement
FROM hotels h
         CROSS JOIN (VALUES ('Wifi'), ('Parking'), ('Restaurant'), ('Bar')) AS e(equipement)
WHERE h.nom = 'Le Peyrou'
  AND NOT EXISTS (SELECT 1 FROM hotel_equipements he WHERE he.hotel_id = h.id);

INSERT INTO hotel_equipements (hotel_id, equipement)
SELECT h.id, e.equipement
FROM hotels h
         CROSS JOIN (VALUES ('Wifi'), ('Climatisation'), ('Bar')) AS e(equipement)
WHERE h.nom = 'La Comedie'
  AND NOT EXISTS (SELECT 1 FROM hotel_equipements he WHERE he.hotel_id = h.id);

INSERT INTO hotel_equipements (hotel_id, equipement)
SELECT h.id, e.equipement
FROM hotels h
         CROSS JOIN (VALUES ('Wifi'), ('Piscine'), ('Spa'), ('Parking'), ('Restaurant'), ('Salle de sport')) AS e(equipement)
WHERE h.nom = 'Antigone Plaza'
  AND NOT EXISTS (SELECT 1 FROM hotel_equipements he WHERE he.hotel_id = h.id);

INSERT INTO hotel_equipements (hotel_id, equipement)
SELECT h.id, e.equipement
FROM hotels h
         CROSS JOIN (VALUES ('Wifi'), ('Parking'), ('Restaurant'), ('Climatisation')) AS e(equipement)
WHERE h.nom = 'Port Marianne'
  AND NOT EXISTS (SELECT 1 FROM hotel_equipements he WHERE he.hotel_id = h.id);

INSERT INTO hotel_equipements (hotel_id, equipement)
SELECT h.id, e.equipement
FROM hotels h
         CROSS JOIN (VALUES ('Wifi'), ('Parking')) AS e(equipement)
WHERE h.nom = 'Les Pres d''Arenes'
  AND NOT EXISTS (SELECT 1 FROM hotel_equipements he WHERE he.hotel_id = h.id);

-- ───────────────────────────────
-- 3 chambres par hotel
-- ───────────────────────────────

-- Le Peyrou
INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Chambre Standard', 130.00, 2, 22, 'Lit Double', 'Chambre cosy avec vue cour interieure.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Le Peyrou'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Chambre Standard');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Suite Promenade', 220.00, 2, 38, 'Lit King Size', 'Suite avec balcon sur la Promenade du Peyrou.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Le Peyrou'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Suite Promenade');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Familiale 4 personnes', 180.00, 4, 45, '2 Lits Doubles', 'Chambre familiale spacieuse avec deux lits doubles.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Le Peyrou'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Familiale 4 personnes');

-- La Comedie
INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Chambre Place', 130.00, 2, 20, 'Lit Queen Size', 'Vue directe sur la place de la Comedie.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'La Comedie'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Chambre Place');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Chambre Cote Cour', 105.00, 2, 18, 'Lit Double', 'Plus calme, donne sur la cour interieure.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'La Comedie'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Chambre Cote Cour');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Junior Suite', 200.00, 3, 35, 'Lit King Size + Canape Lit', 'Suite avec coin salon, ideale pour un week-end en duo + 1.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'La Comedie'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Junior Suite');

-- Antigone Plaza
INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Deluxe Vue Lez', 195.00, 2, 28, 'Lit King Size', 'Chambre Deluxe avec vue sur le Lez et acces piscine.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Antigone Plaza'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Deluxe Vue Lez');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Executive', 175.00, 2, 26, 'Lit King Size', 'Chambre Executive, bureau dedie pour les voyages d''affaires.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Antigone Plaza'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Executive');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Penthouse Suite', 380.00, 4, 75, '2 Lits King Size', 'Suite Penthouse avec terrasse privative et jacuzzi.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Antigone Plaza'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Penthouse Suite');

-- Port Marianne
INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Chambre Lez', 110.00, 2, 22, 'Lit Double', 'Chambre design avec vue sur le Lez.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Port Marianne'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Chambre Lez');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Chambre Patio', 95.00, 2, 20, 'Lit Queen Size', 'Donne sur le patio interieur arbore.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Port Marianne'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Chambre Patio');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Suite Marianne', 165.00, 3, 32, 'Lit King Size + Canape Lit', 'Suite avec coin salon, vue degagee.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Port Marianne'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Suite Marianne');

-- Les Pres d'Arenes
INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Chambre Eco', 75.00, 2, 16, 'Lit Double', 'Chambre standard a prix abordable.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Les Pres d''Arenes'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Chambre Eco');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Chambre Famille', 95.00, 4, 28, '1 Lit Double + 2 Lits Simples', 'Chambre famille avec 4 couchages.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Les Pres d''Arenes'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Chambre Famille');

INSERT INTO chambre (nom, prix_par_nuit, capacity, superficie, type_lit, description, hotel_id, date_creation)
SELECT 'Chambre Twin', 80.00, 2, 18, '2 Lits Simples', 'Pour les voyages d''affaires ou amicaux.',
       h.id, CURRENT_TIMESTAMP
FROM hotels h
WHERE h.nom = 'Les Pres d''Arenes'
  AND NOT EXISTS (SELECT 1 FROM chambre c WHERE c.hotel_id = h.id AND c.nom = 'Chambre Twin');

-- ───────────────────────────────
-- Equipements par chambre (un sous-ensemble representatif)
-- ───────────────────────────────

INSERT INTO chambre_equipements (chambre_id, equipement)
SELECT c.id, e.equipement
FROM chambre c
         JOIN hotels h ON c.hotel_id = h.id
         CROSS JOIN (VALUES ('Wifi'), ('TV'), ('Climatisation')) AS e(equipement)
WHERE h.nom IN ('Le Peyrou', 'La Comedie', 'Antigone Plaza', 'Port Marianne')
  AND NOT EXISTS (SELECT 1 FROM chambre_equipements ce WHERE ce.chambre_id = c.id);

INSERT INTO chambre_equipements (chambre_id, equipement)
SELECT c.id, e.equipement
FROM chambre c
         JOIN hotels h ON c.hotel_id = h.id
         CROSS JOIN (VALUES ('Wifi'), ('TV')) AS e(equipement)
WHERE h.nom = 'Les Pres d''Arenes'
  AND NOT EXISTS (SELECT 1 FROM chambre_equipements ce WHERE ce.chambre_id = c.id);
