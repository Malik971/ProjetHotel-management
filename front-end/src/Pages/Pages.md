# Pages — Vues principales de l'application

> `front-end/src/Pages/`

---

## Principe

Une Page est un **assembleur** : elle récupère les données, gère l'état local, et orchestre l'affichage via des composants.  
Une Page ne contient **jamais** de logique d'appel API inline (`fetch(...)`) si un hook ou service existe déjà pour ça.

---

## `HomePage.jsx`

**Route :** `/`  
**Rôle :** page principale de recherche et de navigation dans les hôtels.

### État local géré
| State | Type | Rôle |
|-------|------|------|
| `allHotels` | `Hotel[]` | Référence immuable chargée au démarrage |
| `displayedHotels` | `Hotel[]` | Liste filtrée/triée affichée à l'écran |
| `showMap` | `boolean` | Bascule entre vue liste et vue carte |
| `selectedHotelId` | `number\|null` | Hôtel cliqué sur la carte |
| `currentFilters` | `object` | Filtres actifs (prix, catégorie, équipements, tri) |

### Flux de données
```
useEffect (montage)
    │
    ▼
getAllHotels()        ← hook useHotelSearch
    │
    ▼
setAllHotels + setDisplayedHotels

Utilisateur tape dans BarRecherche
    │
    ▼
handleSearch() → searchHotels() → hotels (hook)
    │
    ▼
useEffect [hotels] → applyFilters()

Utilisateur change les filtres
    │
    ▼
handleFilterChange() → applyFilters(allHotels, filters)
```

### `applyFilters(hotelsToFilter, filters)`
Fonction centrale — filtre `hotelsToFilter` selon les critères actifs puis trie.  
**Règles importantes :**
- Comparaison équipements en `.toLowerCase()` des deux côtés (la BDD retourne `"Wifi"`, le filtre envoie `"wifi"` → sans ça, zéro résultat)
- Le tri est appliqué **après** le filtrage, pas avant
- Si `filters` est vide, tout est affiché sans transformation

### Calcul de distance Haversine
La fonction `calculateDistance(lat1, lon1, lat2, lon2)` est incluse directement dans la Page (non externalisée) car elle n'est utilisée que dans `applyFilters`. Elle retourne la distance en km entre deux points GPS.

---

## `DetailsPage.jsx`

**Route :** `/hotel/:hotelId`  
**Rôle :** page de détail d'un hôtel avec slider, équipements, description, et liste des chambres.

### Paramètre URL
`hotelId` est récupéré via `useParams()`. Il est passé à `HotelRooms` sous forme **numérique** (`parseInt(hotelId)`) — le composant attend un `number`, pas une `string`.

### Slider d'images
Géré localement avec `currentSlide` (index entier). Actuellement, `slides` contient une seule image (`hotel.imageUrl`). Pour étendre au multi-images, il suffira de remplacer le tableau `slides` par `hotel.imageUrls` quand le backend exposera ce champ.

```js
const slides = hotel.imageUrl ? [hotel.imageUrl] : ["placeholder"];
// → À remplacer par : const slides = hotel.imageUrls || [hotel.imageUrl];
```

### Colonne droite — carte récapitulatif
Sticky (`top-6`) pour rester visible au scroll. Contient :
- Prix minimum par nuit
- Bouton **"Voir les disponibilités"** → redirige vers `/login` (réservation requiert authentification)
- Lien "Voir sur la carte" → ouvre Google Maps avec les coordonnées GPS de l'hôtel
- Badge offre spéciale (statique pour l'instant)

### `equipementIcon(nom)`
Fonction locale qui mappe un nom d'équipement vers une icône Lucide React.  
Utilise des `includes()` sur le nom en minuscule — robuste aux variations de casse BDD.

---

## Pages à créer (prochaines étapes)

| Page | Route | Description |
|------|-------|-------------|
| `LoginPage` | `/login` | Formulaire connexion → JWT |
| `RegisterPage` | `/register` | Création de compte |
| `MonEspacePage` | `/mon-espace` | Mes réservations, annulations |
| `AdminPage` | `/admin` | Dashboard gestion hôtels/users |