# Changelog — Historique des modifications

> Ce fichier trace ce qui a été ajouté, corrigé ou refactorisé au fil du développement.  
> Format : `[composant/fichier] — ce qui a changé et pourquoi.`

---

## Sprint 1 — Fondations & Architecture

### Backend — Entités JPA
- Création du modèle complet : `Hotel`, `Chambre`, `Reservation`, `Users`, `Role`, `Privilege`
- Relations bidirectionnelles Hotel ↔ Chambre ↔ Reservation avec méthodes utilitaires `addChambre()`, `removeReservation()`
- `@JsonIgnore` et `@JsonBackReference` pour éviter les boucles de sérialisation cyclique
- `@PrePersist` sur Hotel, Chambre, Reservation pour horodatage automatique à la création

### Backend —  Controllers
- `HotelController` : CRUD + upload image multipart
- `ChambreController` : CRUD + endpoint disponibilités
- `ReservationController` + `ClientReservationController` : création et consultation des réservations
- `LoginController` + `RegistrationLoginController` : authentification basique + inscription
- `AdminUserController` : gestion des utilisateurs

### Infrastructure
- Flyway activé pour les migrations versionnées
- MySQL configuré en source de données
- `spring-boot-mail` intégré (emails de confirmation prêts à câbler)

---

## Sprint 2 — Frontend initial

### `HomePage.jsx`
- Chargement initial de tous les hôtels au montage
- Intégration du hook `useHotelSearch` (recherche + chargement)
- Bascule liste / carte avec `showMap`
- `applyFilters()` côté client (prix, catégorie, équipements, ville, GPS)
- Calcul de distance Haversine intégré pour le filtre rayon GPS

### `CardHotel.jsx` — v1
- Affichage image + nom + ville + note + lien vers détail
- Fallback image avec `onError` + `onerror = null` pour stopper la boucle

### `Filter.jsx` — v1
- Filtres prix (range), catégorie (checkboxes), équipements (checkboxes)
- Boutons "Appliquer" et "Réinitialiser"

---

## Sprint 3 — Corrections critiques & améliorations

### `HotelController.java` — bug PUT corrigé
**Problème :** `updateHotelJson()` ne mappait pas `latitude`, `longitude`, `prixMoyenNuit`, `categorie`, `equipements`.  
**Conséquence :** impossible de mettre à jour les coordonnées GPS via Postman → marqueurs absents sur la carte.  
**Fix :** ajout des 5 setters manquants dans la méthode PUT.

### `HotelMap.jsx` — réécriture complète
**Problème :** le composant original importait des éléments de page entière, créant un "site dans le site" (doublon de HomePage dans la vue carte).  
**Fix :** réécriture en composant *dumb* pur — reçoit `hotels[]`, `onHotelClick`, `selectedHotelId`. Aucun état propre sauf `MapFocus` (interne).  
**Ajout :** `MapFocus` avec `useMap()` pour `flyTo()` animé sur l'hôtel sélectionné.

### Bug page blanche sur vue carte
**Erreur :** `Invalid hook call — Cannot read properties of null (reading 'useState')`  
**Cause :** deux instances de React dans le bundle (conflit react-leaflet + Vite).  
**Fix :** `resolve: { dedupe: ['react', 'react-dom'] }` dans `vite.config.js` + `rm -rf node_modules && npm install`.  
**Fix alternatif :** import `lazy` + `Suspense` sur `HotelMap` dans `HomePage`.

### `Filter.jsx` + `HomePage.applyFilters` — bug filtres silencieux
**Problème 1 :** Filter émettait `equipments` (anglais), HomePage cherchait `equipements` (français) → 0 résultat sans erreur.  
**Problème 2 :** Filter émettait `categorie` (array de numbers), HomePage cherchait `categories` (clé différente) → idem.  
**Problème 3 :** Casse — Filter envoyait `"wifi"`, BDD retournait `"Wifi"` → jamais égaux.  
**Fix :** alignement des clés sur `equipements` et `categorie` partout + comparaison `.toLowerCase()` des deux côtés.

### `CardHotel.jsx` — v2 redesign
**Changements :**
- Étoiles remplacées par `EtoilesHotel` (★ amber, même style que Filter) — abandon de `<Star>` Lucide qui faisait "trop joué"
- Bouton "Voir détails" → **"Voir les chambres →"** (plus explicite sur ce qu'on trouve derrière)
- Ajout pills équipements (max 3), description tronquée `line-clamp-2`
- Badge catégorie en overlay sur l'image

### `Filter.jsx` — v2 refonte + tri
**Changements :**
- Ajout dropdown **"Trier par"** : pertinence, prix croissant/décroissant, mieux notés, A→Z
- Étoiles redesignées (★ amber + label texte : Luxe / Premium / Confort / Économique)
- Ajout catégorie 2★ (Économique)
- Valeurs équipements alignées sur la casse BDD (`"Wifi"` pas `"wifi"`)
- Style revu : uppercase tracking-wide pour les labels de section

### `DetailsPage.jsx` — redesign complet
**Changements :**
- Slider d'images (extensible multi-images quand BDD exposera `imageUrls[]`)
- Colonne droite sticky avec prix, bouton → `/login`, lien Google Maps
- Bouton "Voir les disponibilités" redirige vers login (réservation requiert authentification)
- Équipements en grille 2 colonnes avec icônes Lucide dynamiques (`equipementIcon()`)
- Badges catégorie et "Recommandé" si `noteMoyenne >= 4.5`
- Badge "Offre spéciale" (statique, extensible)

---

## Prochaines étapes planifiées

- [ ] **JWT** : remplacer l'authentification session par un token JWT stateless
- [ ] **GlobalExceptionHandler** : `@ControllerAdvice` pour des erreurs API cohérentes en JSON
- [ ] **HotelService** : migrer `HotelController` pour passer par une couche service
- [ ] **Swagger UI** : `springdoc-openapi-starter-webmvc-ui` sur `/swagger-ui`
- [ ] **useAuth hook** : état de connexion global (Context React)
- [ ] **MonEspacePage** : mes réservations + annulation
- [ ] **Tests unitaires** : les starters sont déjà dans `pom.xml`, écrire les premières classes de test
- [ ] **Multi-images chambres** : exploiter `imageUrls[]` dans le slider de `DetailsPage`
- [ ] **Recherche géospatiale** : filtre "rayon en km" côté backend (Haversine SQL)