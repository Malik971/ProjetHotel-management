# Dashboard SpringHotel × Pastell

Visualisation temps réel du flux bidirectionnel entre Sejour et Pastell.

## Lancement

```bash
cd dashboard
python -m http.server 5500
```

Puis ouvrir <http://localhost:5500>.

Pré-requis : avoir `sejour-backend` (port 8080) et `pastell-mock` (port 8090) qui tournent en local.

## Structure du projet

```
dashboard/
├── index.html              # Structure HTML pure, aucun style ni script inline
├── README.md               # Ce fichier
└── assets/
    ├── css/
    │   ├── variables.css   # Design tokens (couleurs, polices)
    │   ├── layout.css      # Structure (header, grilles, panneaux)
    │   └── components.css  # Composants (boutons, badges, animations)
    └── js/
        ├── config.js       # Lecture de la config utilisateur (URLs, credentials)
        ├── state.js        # État applicatif global partagé
        ├── api.js          # Tous les appels HTTP (Sejour + Pastell-mock)
        ├── ui.js           # Rendu DOM, mise à jour d'affichage
        └── main.js         # Orchestration et événements
```

## Pourquoi ce découpage

Le projet aurait pu tenir dans un seul fichier HTML monolithique de 900 lignes (la première version l'était). On a préféré séparer pour trois raisons.

### Maintenance

Modifier une couleur du thème ne se fait qu'à un endroit (`variables.css`), pas dans 47 lignes différentes du HTML.

### Lisibilité

Chaque fichier a une responsabilité unique. Quand on ouvre `api.js`, on sait qu'on va voir des appels HTTP, rien d'autre. Quand on ouvre `ui.js`, on sait qu'on va voir du rendu DOM. C'est le **principe de responsabilité unique**, fondateur en architecture logicielle.

### Évolutivité

Si demain on veut migrer l'UI vers React/Vue, on remplace `ui.js`. Si on veut changer de backend, on remplace `api.js`. Les autres fichiers ne bougent pas.

## Ordre des dépendances

L'ordre de chargement dans `index.html` n'est pas anodin :

```
config.js  →  state.js  →  api.js  →  ui.js  →  main.js
```

Chaque module utilise les exports du précédent :

- `api.js` utilise `config.sejour()` et `config.mockHeaders()`
- `ui.js` utilise `state.resaStatut` et `state.pastellAction`
- `main.js` utilise `api.fetchReservation()` et `ui.renderReservation()`

L'attribut `defer` sur les balises `<script>` garantit que les fichiers s'exécutent dans l'ordre, après le parsing du HTML.

## Communication entre modules

On n'utilise pas de `import`/`export` ES6 modules pour rester compatible avec un simple serveur de fichiers statiques sans configuration. À la place, chaque fichier expose ses fonctions sur l'objet global `window` :

```javascript
window.config = { sejour: ..., mock: ... };
window.api    = { fetchReservation: ..., fetchJournal: ... };
window.ui     = { renderReservation: ..., showError: ... };
window.state  = { resaId: null, idD: null, ... };
```

C'est une convention claire : majuscule pour les classes, minuscule pour les modules. Pour un projet plus gros, on passerait à un bundler (Vite, Webpack, esbuild) qui gérerait `import`/`export`.

## Comment ça communique avec les backends

```
┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│                  │  GET  │                  │       │                  │
│   Dashboard HTML │──────▶│   sejour-backend │──────▶│   PostgreSQL     │
│   (port 5500)    │  POST │   (port 8080)    │       │                  │
│                  │       │                  │       └──────────────────┘
│                  │       └──────────────────┘
│                  │       ┌──────────────────┐
│                  │  GET  │                  │
│                  │──────▶│   pastell-mock   │
│                  │  POST │   (port 8090)    │
└──────────────────┘       └──────────────────┘
```

- **Vers Sejour** : pas d'authentification (endpoints `/api/admin/**` ouverts pour la démo)
- **Vers Pastell-mock** : Basic Auth avec les credentials saisis dans la barre de configuration

## Pour aller plus loin

- Pour ajouter un nouveau bouton d'action, modifier `ui.js` (fonction `renderActionButtons`)
- Pour pointer vers un autre backend, changer la valeur dans la barre de config (sans toucher au code)
- Pour changer le thème, modifier les valeurs CSS dans `variables.css`
- Pour ajouter une nouvelle métrique au panneau de cohérence, modifier `ui.js` (fonction `updateCoherenceIndicator`)