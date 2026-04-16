# Components — Composants réutilisables

> `front-end/src/components/`

---

## Philosophie

Un composant est **muet sur les données** : il reçoit des props, il affiche.  
Il ne fetche jamais, ne filtre jamais, ne connaît pas l'état global.  
Si un composant a besoin de logique complexe, elle monte dans la Page ou dans un hook.

---

## `CardHotel.jsx`

**Usage :** grille de résultats sur `HomePage`  
**Props :** `hotel` (objet Hotel complet)

### Ce qu'il fait
Affiche la carte d'un hôtel : image, badge catégorie, nom, ville, étoiles, pills équipements, description tronquée, prix, bouton de navigation.

### Points techniques
- **Image fallback :** si `hotel.imageUrl` commence par `http`, utilisée directement. Sinon, préfixée par `VITE_API_URL` (images uploadées localement). Si l'image échoue au chargement, remplacée par un placeholder Placehold.co.
- **`onError` avec `e.target.onerror = null`** : stoppe la boucle infinie (sans ça, le fallback lui-même peut déclencher un nouvel `onError`).
- **Étoiles** : composant interne `EtoilesHotel` utilise `hotel.categorie` (nombre d'étoiles 1-5) avec des `★` en amber. **Même style que le filtre** pour cohérence visuelle.
- **Équipements** : `hotel.equipements` (clé française, alignée avec la BDD). Maximum 3 pills affichées.
- **Bouton :** "Voir les chambres →" → navigue vers `/hotel/{id}` via `<Link>`.

### Alignement clé BDD ↔ composant
```
BDD retourne :  { equipements: ["Wifi", "Parking"] }
CardHotel lit : hotel.equipements   ← ✅ aligné
```

---

## `Filter.jsx`

**Usage :** sidebar gauche sur `HomePage`  
**Props :** `onFilterChange(filters)`, `onReset()`

### Ce qu'il fait
Expose 4 critères de filtrage + 1 tri, appliqués en batch sur le bouton "Appliquer".

### Structure de l'objet `filters` émis
```js
{
  prixMax: 500,           // number — prix plafond par nuit
  categorie: [4, 5],      // number[] — étoiles sélectionnées
  equipements: ["Wifi"],  // string[] — noms exacts comme en BDD (casse respectée)
  notationMin: 0,         // number — note minimale
  tri: "prix_asc"         // string — clé de tri
}
```

### Valeurs `tri` reconnues par `HomePage.applyFilters`
| Valeur | Effet |
|--------|-------|
| `""` | Ordre d'origine (pertinence) |
| `"prix_asc"` | Prix croissant |
| `"prix_desc"` | Prix décroissant |
| `"note_desc"` | Mieux notés en premier |
| `"nom_asc"` | Alphabétique A→Z |

### Alignement clé filtre ↔ BDD ↔ HomePage
```
Filter émet :       { equipements: ["Wifi"] }   ← clé française
HomePage cherche :  hotel.equipements            ← même clé
BDD retourne :      "Wifi"                       ← même casse
Comparaison :       .toLowerCase() des deux côtés ← sécurité supplémentaire
```

**Avant la correction :** Filter émettait `equipments` (anglais) et `categorie` (singulier), HomePage cherchait `equipements` et `categories` → 0 résultat garanti.

---

## `HotelMap.jsx`

**Usage :** vue carte sur `HomePage`  
**Props :** `hotels[]`, `onHotelClick(hotel)`, `selectedHotelId`

### Ce qu'il fait
Affiche une carte interactive OpenStreetMap (via **Leaflet + react-leaflet**) avec un marqueur par hôtel ayant des coordonnées GPS.

### Prérequis
```bash
npm install leaflet react-leaflet
```

Le fix d'icône Leaflet est **obligatoire avec Vite** :
```js
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({ iconUrl: "...", ... });
```
Sans ça, les marqueurs n'affichent pas d'icône (Webpack/Vite ne résout pas automatiquement les assets Leaflet).

### Composant interne `MapFocus`
Utilise le hook `useMap()` de react-leaflet pour accéder à l'instance de la carte.  
Quand `selectedHotelId` change → `map.flyTo()` avec animation vers les coordonnées de l'hôtel sélectionné.  
**Pourquoi un composant séparé ?** `useMap()` ne peut être appelé qu'à l'intérieur d'un enfant de `<MapContainer>`.

### Popup au clic sur un marqueur
Affiche : image de l'hôtel (si disponible), nom, ville, prix/nuit, note.

### Hôtels sans GPS
Filtrés automatiquement (`latitude != null && longitude != null`). Un message est affiché si aucun hôtel de la liste n'a de coordonnées.

### Résolution du bug page blanche
L'erreur `Invalid hook call` venait de deux copies de React dans le bundle (conflit react-leaflet + Vite).  
**Fix :** `resolve: { dedupe: ['react', 'react-dom'] }` dans `vite.config.js`.  
**Fix alternatif :** import lazy + Suspense dans HomePage.

---

## `DetailPages/HotelRooms.jsx`

**Usage :** section chambres sur `DetailsPage`  
**Props :** `hotelId` (number)

### Ce qu'il fait
Charge les chambres d'un hôtel via `useChambres(hotelId)` et affiche une grille de `RoomCard`.  
Gère l'état `selectedChambre` pour ouvrir le `ReservationModal`.

### Point d'attention
`hotelId` doit être passé en **number** depuis `DetailsPage` :
```jsx
<HotelRooms hotelId={parseInt(hotelId)} />  // ← parseInt obligatoire
```
`useParams()` retourne toujours une string — si le hook `useChambres` compare avec `===`, un string `"1"` ≠ number `1`.

---

## `DetailPages/ReservationModal.jsx`

**Usage :** modal de réservation sur `DetailsPage`  
**Props :** `chambre`, `onClose()`, `onSuccess(reservation)`

### Ce qu'il fait
Formulaire complet de réservation : dates d'arrivée/départ, informations client, nombre de personnes.  
Calcule le prix estimé en temps réel à partir de `chambre.prixParNuit` × nombre de nuits.

### Accès restreint
Ce composant ne doit s'afficher que pour un utilisateur connecté. `DetailsPage` redirige vers `/login` avant d'afficher le bouton de réservation — le modal n'est plus accessible sans authentification.

### Appel API
Délègue à `reservationService.creerReservation(payload)`.  
En cas de succès → `alert()` avec le code de confirmation + callback `onSuccess` + fermeture du modal.