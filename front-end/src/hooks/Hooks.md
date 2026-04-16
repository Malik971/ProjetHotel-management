# Hooks & Services — Logique métier côté client

> `front-end/src/hooks/` · `front-end/src/services/`

---

## Philosophie

Les **hooks** encapsulent la logique d'état liée à une ressource (hotels, chambres…).  
Les **services** encapsulent les appels HTTP bruts vers l'API.

```
Page
  │
  └── useHotelSearch()        ← hook (état + appel service)
          │
          └── fetch(...)      ← appel HTTP direct (ou service)
```

Un hook retourne `{ data, loading, error, actions }`.  
Une Page ne devrait jamais écrire `fetch(...)` directement si un hook/service existe.

---

## `useHotelSearch.js`

**Utilisé par :** `HomePage`

### Ce qu'il expose
```js
const {
  hotels,       // Hotel[] — résultats de la dernière recherche
  loading,      // boolean
  error,        // string | null
  searchHotels, // (searchParams) => Promise<void>
  getAllHotels,  // () => Promise<void>
} = useHotelSearch();
```

### `getAllHotels()`
Appelle `GET /api/hotels/all` et met à jour `hotels`.  
Déclenché au montage de `HomePage` pour charger la liste initiale.

### `searchHotels(searchParams)`
Appelle `POST /api/hotels/search` avec un body JSON contenant les critères.  
Le shape de `searchParams` correspond à `HotelSearchDTO` côté backend.

### Relation avec `applyFilters`
`useHotelSearch` ne filtre pas — il retourne les résultats bruts du backend.  
C'est `HomePage.applyFilters()` qui applique ensuite les filtres côté client (prix, catégorie, équipements, tri).  
**Pourquoi les deux ?** La recherche backend filtre sur ville/dates/capacité (lourd en SQL). Les filtres front filtrent en temps réel sans requête supplémentaire.

---

## `useChambres.js`

**Utilisé par :** `HotelRooms`

### Ce qu'il expose
```js
const {
  chambres,   // Chambre[]
  loading,    // boolean
  error,      // string | null
} = useChambres(hotelId);
```

### Comportement
Déclenché à chaque changement de `hotelId`.  
Appelle `GET /api/chambres/hotel/{hotelId}`.  
`hotelId` doit être un **number** — le hook ne fait pas de conversion.

---

## `services/reservationService.js`

**Utilisé par :** `ReservationModal`

### Ce qu'il expose
```js
reservationService.creerReservation(payload) // → Promise<ReservationResponse>
```

### Shape du payload
```js
{
  chambreId: 3,
  dateDebut: "2026-06-10",
  dateFin: "2026-06-13",
  nomClient: "Jean Dupont",
  emailClient: "jean@example.com",
  telephoneClient: "0612345678",
  nombrePersonnes: 2
}
```

### Authentification
Si l'utilisateur est connecté, le service doit inclure le token JWT dans les headers :
```js
Authorization: `Bearer ${localStorage.getItem('token')}`
```
**À implémenter** quand le backend passera en JWT.

---

## Pattern à suivre pour les prochains hooks

```js
// Template de hook
import { useState, useEffect } from "react";

export function useMonRessource(param) {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!param) return;
    setLoading(true);

    fetch(`${import.meta.env.VITE_API_URL}/api/ma-ressource/${param}`)
      .then((res) => {
        if (!res.ok) throw new Error(`Erreur ${res.status}`);
        return res.json();
      })
      .then((json) => { setData(json); setLoading(false); })
      .catch((err) => { setError(err.message); setLoading(false); });
  }, [param]);

  return { data, loading, error };
}
```

---

## Hooks à créer

| Hook | Ressource | Déclenché par |
|------|-----------|---------------|
| `useReservations` | Mes réservations client | `MonEspacePage` |
| `useHotelAdmin` | CRUD hôtels admin | `AdminPage` |
| `useAuth` | État connexion / token JWT | Global (Context) |