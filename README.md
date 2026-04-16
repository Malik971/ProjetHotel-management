# Application Web « Réservation Hôtel »

## 1. Contexte et objectif

#### Créer une application web de réservation hôtelière permettant à des clients de :

● consulter la liste des hôtels disponibles,
● rechercher un hôtel selon différents critères (localisation, prix, notation,
équipements…),
● visualiser la position des hôtels sur une carte interactive,
● réserver une chambre pour une période donnée,
● être notifié ou placé en liste d’attente si aucune chambre n’est disponible à la date
souhaitée.

#### L’application doit aussi permettre à un administrateur de gérer :

● les hôtels (création, modification, suppression),
● les chambres et leurs disponibilités,
● les réservations et les comptes clients.

# SpringHotel - Plateforme de réservation hôtelière

> Stack : **Spring Boot 4.0 · Java 25 · React 18 · Tailwind CSS · MySQL · Flyway**
 
---

## Vision du projet

SpringHotel est une application full-stack de gestion et de réservation hôtelière pensée autour de **Montpellier et sa région**.  
Elle cible trois profils d'utilisateurs simultanément :

| Profil | Ce qu'il peut faire |
|--------|---------------------|
| **Visiteur** | Rechercher, filtrer, consulter les hôtels et leurs chambres |
| **Client connecté** | Réserver, consulter et annuler ses réservations |
| **Administrateur** | Gérer les hôtels, chambres et utilisateurs via API sécurisée |
 
---

## Architecture globale

```
springhotel/
├── backend/                    ← Spring Boot (API REST)
│   └── src/main/java/
│       └── com/example/springhotel/
│           ├── entity/         ← Modèle JPA (Hotel, Chambre, Reservation, Users…)
│           ├── controller/     ← REST Controllers (points d'entrée HTTP)
│           ├── service/        ← Logique métier
│           ├── repository/     ← Requêtes JPA / base de données
│           └── dto/            ← Objets de transfert (entrée/sortie API)
│
├── front-end/                  ← React + Vite
│   └── src/
│       ├── Pages/              ← Vues principales (HomePage, DetailsPage…)
│       ├── components/         ← Composants réutilisables
│       ├── hooks/              ← Logique métier côté client (useHotelSearch…)
│       └── services/           ← Appels API (reservationService…)
│
└── README.md                   ← Ce fichier
```
 
---

## Flux de données — comment tout s'emboîte

```
[Utilisateur]
     │
     ▼
[React Frontend]  →  fetch / axios  →  [Spring Security]
                                              │
                                        Authentification
                                              │
                                    [REST Controllers]
                                              │
                                       [Services]
                                              │
                                    [Repositories JPA]
                                              │
                                          [MySQL]
```

**Règle clé :** le front ne parle jamais directement à la base.  
Tout passe par l'API REST exposée sur `http://localhost:8080`.
 
---

## Démarrage rapide

### Backend
```bash
# Prérequis : Java 25, MySQL en local
cd backend
./mvnw spring-boot:run
# API disponible sur http://localhost:8080
```

### Frontend
```bash
cd front-end
npm install
npm run dev
# App disponible sur http://localhost:5173
```

### Variables d'environnement (`.env`)
```env
VITE_API_URL=http://localhost:8080
```
 
---

## Fonctionnalités implémentées

- [x] Recherche d'hôtels par ville, dates, capacité
- [x] Filtres combinables : prix, catégorie (étoiles), équipements
- [x] Tri par prix, note, nom
- [x] Vue carte interactive (Leaflet / OpenStreetMap) avec marqueurs GPS
- [x] Page détail hôtel avec slider d'images
- [x] Liste des chambres par hôtel
- [x] Système de réservation (authentification requise)
- [x] Espace client : mes réservations, annulation
- [x] Panel admin : gestion users, hôtels, chambres
- [x] Emails de confirmation (spring-boot-mail)
- [x] Migrations BDD versionnées (Flyway)
## Ce qui arrive

- [ ] JWT stateless (remplace la session actuelle)
- [ ] Swagger UI / OpenAPI 3 sur `/swagger-ui`
- [ ] Recherche géospatiale (Haversine — rayon en km)
- [ ] Export PDF de confirmation de réservation
- [ ] Moteur de recommandation basé sur l'historique
---

## Conventions de développement

- **Backend :** un Controller ne parle jamais au Repository directement — il passe toujours par un Service (sauf `HotelController` en cours de refactoring).
- **Frontend :** la logique d'appel API vit dans `/hooks` ou `/services`, jamais directement dans un composant Page.
- **Nommage :** français pour les variables métier (`prixMoyenNuit`, `dateDebut`), anglais pour les utilitaires techniques (`handleSubmit`, `loading`).
---

*Voir les READMEs dans chaque sous-dossier pour le détail de chaque couche.*

### Technologies cibles : Frontend React, Backend Java Spring Boot, Base de données relationnelle (PostgreSQL ou MySQL).

2. Suivi du projet

[Pour plus de d'étail sur notre projet à Mohamed et Malik](https://docs.google.com/document/d/1Lh5e2OUFWu4cGZceroKN7-JqxY0jcAA2zcEmHkMi7PE/edit?tab=t.0)

Nous avons commencé par analyser le cahier des charges, puis créé un tableau de bord sur [Trello](https://trello.com/b/9Iz00TDD/projet-hotel)
pour centraliser nos idées, nos tâches et le suivi de l’avancement.

Ensuite, nous avons réalisé plusieurs [diagrammes UML](https://drive.google.com/file/d/1azgBVfcXhUdf6qLXU8zJQ5138nK52xJ-/view?ts=69120c94) afin de mieux comprendre la structure du projet, avant de démarrer une [maquette sur Figma](https://www.figma.com/site/0BG3Y7PA3CXIIbPyeyrKhX/Projet-Hotel?node-id=0-1&p=f&t=umikHzEYExZ32Sie-0)
pour visualiser l’interface utilisateur.

3. Démarrage du projet (10/11/2025)

Le projet est développé en deux parties :

Frontend : initialisé avec React.js → [Documentation officielle](https://react.dev/learn/creating-a-react-app)

Style : configuré avec TailwindCSS → [Guide d’installation](https://tailwindcss.com/docs/installation/using-vite)

Backend : lancé avec Spring Boot → [Documentation officielle](https://start.spring.io/)

Nous avons donc démarré le projet avec React pour le front-end et Spring Boot pour le back-end, en mettant en place les bases de l’architecture et du design.

Les contributeurs:

[Mohamed Benchrif](https://github.com/azerkane44)
[Malik Ibo](https://github.com/Malik971)
