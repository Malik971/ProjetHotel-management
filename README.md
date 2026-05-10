# SpringHotel

Plateforme de réservation hôtelière sur Montpellier et sa région, avec une intégration Pastell pour la dématérialisation des dossiers de réservation.

Stack : Spring Boot 4.0, Java 21, React 18, Tailwind CSS, PostgreSQL, Flyway.

## Pourquoi ce projet

SpringHotel est mon projet portfolio. Je l'ai construit pour démontrer une capacité à concevoir, structurer et faire évoluer une application full-stack non triviale, et pour préparer ma candidature au poste de DevRel chez Libriciel SCOP. C'est pour cette raison que j'y ai intégré Pastell, le produit phare de Libriciel : pour montrer que je sais lire une API, l'instrumenter, la documenter, et construire des partenaires d'intégration robustes autour.

## Ce que l'application fait aujourd'hui

Trois profils d'utilisateurs cohabitent.

Un visiteur peut consulter la liste des hôtels disponibles, les filtrer par prix, catégorie, équipements, les voir sur une carte interactive Leaflet centrée sur Montpellier, et accéder au détail de chaque chambre.

Un client connecté peut réserver une chambre pour une période donnée, recevoir un mail de confirmation, consulter l'historique de ses réservations, et les annuler.

Un administrateur peut gérer les hôtels, chambres et utilisateurs via une API sécurisée.

L'intégration Pastell, ajoutée par lots successifs, permet à chaque création de réservation de générer automatiquement un dossier dématérialisé côté Pastell, et inversement, de répercuter en temps quasi-réel sur la réservation Sejour les actions effectuées par un agent dans Pastell (validation, annulation, terminaison).

## Architecture du dépôt

```
springhotel/
├── back-end/
│   ├── sejour-backend/     application Spring Boot principale (port 8080)
│   └── pastell-mock/       mock Pastell pour développement local (port 8090)
│
├── front-end/              React + Vite (port 5173)
│
└── dashboard/              dashboard HTML pour visualiser le flux Sejour ↔ Pastell
```

Le projet est organisé en multi-module Maven. Le module `pastell-mock` reproduit fidèlement le comportement de l'API Pastell réelle pour permettre un développement local et des tests d'intégration sans dépendance externe.

## Démarrage rapide

### Prérequis

Java 21, Node 18+, PostgreSQL 17, Maven 3.9+.

Une base PostgreSQL nommée `hotel_db` doit exister localement. Pour la créer :

```bash
psql -U postgres -h localhost
CREATE DATABASE hotel_db;
\q
```

Flyway s'occupera de créer toutes les tables au premier démarrage.

### Configuration des variables d'environnement

Copie les fichiers d'exemple dans chaque module :

```bash
cp back-end/sejour-backend/.env.example back-end/sejour-backend/.env
cp back-end/pastell-mock/.env.example back-end/pastell-mock/.env
cp front-end/.env.example front-end/.env
```

Édite chaque `.env` pour mettre tes valeurs locales (mot de passe Postgres, app password Gmail, etc.). Les `.env` ne sont pas commités, ils restent strictement locaux.

### Lancer le mock Pastell

```bash
cd back-end/pastell-mock
./mvnw spring-boot:run
```

Le mock écoute sur le port 8090.

### Lancer le backend principal

```bash
cd back-end/sejour-backend
./mvnw spring-boot:run
```

L'API est disponible sur `http://localhost:8080`.

### Lancer le frontend React

```bash
cd front-end
npm install
npm run dev
```

L'application est disponible sur `http://localhost:5173`.

### Lancer le dashboard de démo

```bash
cd dashboard
python -m http.server 5500
```

Ouvre `http://localhost:5500` pour voir la visualisation en temps réel du flux entre Sejour et Pastell.

## L'intégration Pastell en bref

L'intégration a été développée par lots successifs (5 lots à ce jour), chacun ciblant une dimension précise.

Le **lot 1** a posé l'architecture multi-module Maven et la séparation claire entre Sejour et le mock Pastell.

Le **lot 2** a construit le mock Pastell qui implémente fidèlement les endpoints `/api/v2/document` et `/api/v2/journal`, avec un workflow d'états et un système d'authentification HTTP Basic.

Le **lot 3** a câblé la synchronisation montante : à chaque création de réservation côté Sejour, un dossier est automatiquement créé côté Pastell.

Le **lot 4** a ajouté un système de retry à deux niveaux pour rendre la synchronisation montante robuste face aux pannes temporaires de Pastell. Premier niveau de retry court immédiat, deuxième niveau de reprise différée par scheduler.

Le **lot 5** a fermé la boucle avec la synchronisation descendante : Sejour interroge le journal Pastell toutes les 30 secondes pour détecter les actions effectuées par un agent dans Pastell et les répercuter sur les réservations.

La documentation technique de chaque lot se trouve dans le code, à proximité des fichiers concernés. Pour les lots 4 et 5, voir respectivement `back-end/sejour-backend/src/main/java/com/example/springhotel/integration/pastell/RETRY.md` et `POLLING.md`.

## Conventions de développement

Côté backend, un Controller ne parle jamais directement à un Repository. Toute logique passe par une couche Service. La seule exception qui demeure est `HotelController`, qui est un héritage des premiers sprints et est en cours de refactorisation.

Côté frontend, les appels API ne se font jamais directement dans un composant Page. Ils passent par des fonctions exposées dans `services/` ou par des hooks personnalisés dans `hooks/`.

Côté nommage, les variables métier sont en français (`prixMoyenNuit`, `dateDebut`, `nombrePersonnes`) parce que ce sont des concepts du domaine. Les utilitaires techniques restent en anglais (`handleSubmit`, `loading`, `useEffect`).

Côté commits, j'utilise les conventional commits avec une portée explicite : `feat(pastell): ...`, `fix(frontend): ...`, `docs(readme): ...`.

## État actuel et prochaines étapes

Ce qui fonctionne aujourd'hui :

* recherche d'hôtels par ville, dates, capacité ;
* filtres combinables prix, catégorie, équipements ;
* tri par prix, note, nom ;
* vue carte interactive Leaflet avec marqueurs GPS ;
* page détail hôtel avec slider d'images ;
* système de réservation complet ;
* espace client pour gérer ses réservations ;
* panel admin pour les utilisateurs, hôtels, chambres ;
* emails de confirmation via spring-boot-mail ;
* migrations versionnées Flyway ;
* intégration Pastell bidirectionnelle complète (5 lots).

Ce qui est en cours ou prévu :

* lot 6 d'observabilité avec Micrometer et un dashboard métriques ;
* migration de l'authentification de session vers JWT stateless ;
* documentation OpenAPI 3 / Swagger UI ;
* recherche géospatiale par rayon en kilomètres ;
* tests unitaires et d'intégration plus larges.

## Ressources liées

* Suivi du projet : [Trello](https://trello.com/b/9Iz00TDD/projet-hotel)
* Diagrammes UML : [Drive](https://drive.google.com/file/d/1azgBVfcXhUdf6qLXU8zJQ5138nK52xJ-/view)
* Maquettes : [Figma](https://www.figma.com/site/0BG3Y7PA3CXIIbPyeyrKhX/Projet-Hotel)
* Document de cadrage : [Doc Mohamed et Malik](https://docs.google.com/document/d/1Lh5e2OUFWu4cGZceroKN7-JqxY0jcAA2zcEmHkMi7PE/edit)

## Contributeurs

* [Mohamed Benchrif](https://github.com/azerkane44)
* [Malik Ibo](https://github.com/Malik971)

Voir les README spécifiques dans chaque sous-dossier pour le détail de chaque couche.