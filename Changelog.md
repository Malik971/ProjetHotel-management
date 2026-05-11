# Historique des modifications

Ce fichier trace ce qui a été ajouté, corrigé ou refactorisé au fil du développement. Format adopté : par sprint, avec pour chaque entrée le composant ou fichier touché, ce qui a changé, et pourquoi.

## Sprint 1 : fondations et architecture

### Modèle JPA

Création du modèle complet : `Hotel`, `Chambre`, `Reservation`, `Users`, `Role`, `Privilege`. Les relations bidirectionnelles entre Hotel, Chambre et Reservation ont été assorties de méthodes utilitaires (`addChambre()`, `removeReservation()`) pour éviter les incohérences de cohabitation. Les annotations `@JsonIgnore` et `@JsonBackReference` ont été ajoutées aux endroits nécessaires pour éviter les boucles de sérialisation cycliques. Les callbacks `@PrePersist` sur Hotel, Chambre et Reservation gèrent l'horodatage automatique à la création.

### Controllers REST

* `HotelController` : CRUD complet plus upload d'image multipart
* `ChambreController` : CRUD plus endpoint disponibilités
* `ReservationController` et `ClientReservationController` : création et consultation des réservations
* `LoginController` et `RegistrationLoginController` : authentification basique et inscription
* `AdminUserController` : gestion des utilisateurs

### Infrastructure

Flyway activé pour les migrations versionnées. MySQL configuré comme source de données initiale. Le starter `spring-boot-mail` intégré pour préparer les emails de confirmation.

## Sprint 2 : frontend initial

### `HomePage.jsx`

Chargement initial de tous les hôtels au montage du composant. Intégration du hook `useHotelSearch` qui combine recherche et chargement. Bascule entre vue liste et vue carte avec un état `showMap`. Calcul de distance Haversine intégré pour le filtre de rayon GPS.

### `CardHotel.jsx` v1

Affichage simple : image, nom, ville, note, lien vers la page détail. Fallback image avec `onError` plus `onerror = null` pour casser la boucle infinie en cas d'image cassée.

### `Filter.jsx` v1

Premiers filtres : prix sous forme de range, catégorie en checkboxes, équipements en checkboxes. Boutons "Appliquer" et "Réinitialiser".

## Sprint 3 : corrections critiques et améliorations

### Bug PUT sur `HotelController.java`

`updateHotelJson()` ne mappait pas `latitude`, `longitude`, `prixMoyenNuit`, `categorie` et `equipements`. Conséquence visible : impossible de mettre à jour les coordonnées GPS via Postman, donc les marqueurs n'apparaissaient pas sur la carte. Fix : ajout des cinq setters manquants dans la méthode PUT.

### Réécriture complète de `HotelMap.jsx`

Le composant original importait des éléments de page entière, créant un effet de "site dans le site" (un doublon de HomePage apparaissait dans la vue carte). Réécriture en composant *dumb* pur qui reçoit `hotels[]`, `onHotelClick` et `selectedHotelId` en props, sans état interne propre sauf `MapFocus`. Ajout d'un sous-composant `MapFocus` qui utilise `useMap()` pour faire un `flyTo()` animé sur l'hôtel sélectionné.

### Page blanche sur la vue carte

Erreur observée : `Invalid hook call. Cannot read properties of null (reading 'useState')`. Cause identifiée : deux instances de React étaient incluses dans le bundle Vite, à cause d'un conflit avec react-leaflet qui embarque sa propre copie. Fix : ajout de `resolve: { dedupe: ['react', 'react-dom'] }` dans `vite.config.js`, suivi d'un nettoyage complet `rm -rf node_modules && npm install`. Fix complémentaire : import `lazy` plus `Suspense` sur `HotelMap` dans `HomePage` pour découpler le chargement.

### Bug de filtres silencieux

Trois bugs cumulés qui rendaient les filtres inopérants sans erreur visible.

* Filter émettait `equipments` (anglais), HomePage cherchait `equipements` (français), donc 0 résultat sans message d'erreur.
* Filter émettait `categorie` comme array de numbers, HomePage cherchait `categories` au pluriel.
* Le filtre équipements comparait `"wifi"` (envoyé par Filter) à `"Wifi"` (stocké en BDD) sans normalisation, donc jamais d'égalité.

Fix : alignement de toutes les clés sur les versions françaises (`equipements`, `categorie`) côté frontend, et application d'un `.toLowerCase()` des deux côtés de la comparaison.

### `CardHotel.jsx` v2 redesign

* Étoiles refaites avec un composant dédié `EtoilesHotel` (★ amber, même style que dans Filter), abandon du composant `<Star>` Lucide qui faisait "trop joué".
* Bouton "Voir détails" renommé en "Voir les chambres →" (plus explicite sur ce qu'on trouve derrière).
* Ajout de pills équipements (3 maximum), description tronquée avec `line-clamp-2`.
* Badge catégorie en overlay sur l'image.

### `Filter.jsx` v2 refonte et tri

* Ajout d'un dropdown "Trier par" avec cinq options : pertinence, prix croissant, prix décroissant, mieux notés, A→Z.
* Étoiles redesignées avec un label texte associé : Luxe / Premium / Confort / Économique.
* Ajout de la catégorie 2 étoiles (Économique).
* Valeurs équipements alignées sur la casse de la BDD.
* Style revu : labels de section en uppercase tracking-wide.

### `DetailsPage.jsx` redesign complet

* Slider d'images, extensible pour quand la BDD exposera `imageUrls[]`.
* Colonne droite sticky avec prix et bouton qui redirige vers `/login`.
* Bouton "Voir les disponibilités" qui redirige vers login (la réservation requiert authentification).
* Équipements affichés en grille deux colonnes avec icônes Lucide dynamiques via `equipementIcon()`.
* Badges catégorie et "Recommandé" si `noteMoyenne >= 4.5`.
* Badge "Offre spéciale" statique pour préparer une mise en avant ultérieure.

## Sprint 4 : audit et migration PostgreSQL

### Audit de sécurité et architecture

Identification de plusieurs problèmes : authentification JWT manquante, routes admin non protégées, usage du wildcard CORS, mismatch de types `Double` vs `BigDecimal` pour `prixTotal`, appels directs aux repositories depuis les controllers en bypassant la couche service. Établissement d'une roadmap en trois sprints pour traiter ces points.

### Migration MySQL vers PostgreSQL

Plusieurs bugs spécifiques à PostgreSQL ont été corrigés à cette occasion.

* `GenerationType.AUTO` remplacé par `IDENTITY` sur `Users`, `Role` et `Privilege` (compatibilité PostgreSQL).
* `MySQLDialect` qui persistait en production parce que `application.properties` surchargeait le profil prod : fix en mettant explicitement `PostgreSQLDialect` dans `application-prod.properties`.
* `SetupDataLoader` qui se déclenchait avant que Hibernate ait fini de créer les tables : fix en passant de `ContextRefreshedEvent` à `ApplicationReadyEvent`.
* `ddl-auto=create` qui détruisait les données à chaque redémarrage : passage à `update`.
* `application-prod.properties` qui levait une `MalformedInputException` à cause des accents français sauvés en ANSI : fix en supprimant tous les accents des commentaires.

### Refonte des services frontend

Tous les fichiers de services contenaient `localhost:8080` en dur. Fix par remplacement systématique avec `import.meta.env.VITE_API_URL`. Au passage, la version inexistante `^1.8.0` de `lucide-react` qui faisait planter le build Netlify a été corrigée en `0.460.0`.

## Sprints 5 à 9 : intégration Pastell par lots

### Lot 1 : architecture multi-module

Mise en place d'une structure Maven multi-module avec un parent qui agrège deux modules : `sejour-backend` (l'application principale) et `pastell-mock` (le mock pour le développement local). Cette séparation permet de versionner et tester indépendamment chaque module.

### Lot 2 : mock Pastell complet

Construction du mock qui implémente fidèlement les endpoints Pastell pertinents pour ce projet :

* `POST /api/v2/entite/{id}/document` : création d'un dossier
* `GET /api/v2/entite/{id}/document/{idD}` : lecture de l'état courant d'un dossier
* `POST /api/v2/entite/{id}/document/{idD}/action` : transition d'état (validation, confirmation, terminaison, annulation)
* `GET /api/v2/journal` : lecture du journal d'événements depuis un id_j donné

Authentification HTTP Basic, workflow d'états modélisé avec une enum `DocumentTransitions`, journal interne en mémoire avec un `AtomicLong` pour garantir l'unicité des id_j.

### Lot 3 : synchronisation montante

Câblage de la synchronisation Sejour vers Pastell. À chaque création de réservation, un événement `ReservationCreatedEvent` est publié. Un listener `ReservationCreatedListener` capture l'événement après commit de la transaction et déclenche un appel à `PastellSyncService` qui crée le dossier dans Pastell. La table `pastell_sync` mémorise le lien entre une réservation Sejour et le dossier Pastell correspondant.

### Lot 4 : retry à deux niveaux

Ajout d'un mécanisme de retry robuste pour rendre la sync montante résiliente aux pannes temporaires de Pastell.

* Premier niveau : retry court immédiat avec `RetryTemplate` Spring (3 tentatives, backoff exponentiel).
* Deuxième niveau : reprise différée par un scheduler qui passe toutes les 5 minutes pour retraiter les syncs en `EN_RETRY`, jusqu'à un maximum de tentatives totales configurable.

Documentation détaillée disponible dans `RETRY.md`.

### Lot 5 : synchronisation descendante par polling

Fermeture de la boucle avec la sync inverse : Sejour interroge `GET /api/v2/journal` toutes les 30 secondes pour détecter les actions effectuées par un agent dans Pastell, et répercuter ces actions sur les réservations.

* Curseur de polling persistant en base (`pastell_polling_cursor`) pour ne jamais retraiter une entrée déjà vue, même après redémarrage.
* Table de décision `PastellActionMapper` qui mappe les actions Pastell vers les statuts de réservation Sejour.
* Détection de divergence quand Pastell envoie une action incohérente avec le statut Sejour courant : le sync passe en `DIVERGENCE` et un humain doit arbitrer.
* Architecture découpée en orchestrateur (`PastellInboundSyncService`) et processor transactionnel (`PastellJournalEntryProcessor`) pour respecter les contraintes de Spring AOP sur les appels intra-bean.
* 25 nouveaux tests : unitaires Mockito, intégration WireMock, et un test end-to-end complet.

Documentation détaillée disponible dans `POLLING.md`.

### Lot 6 : mise en ligne et observabilite

Fermeture de la phase d'integration Pastell par la mise en ligne publique du projet sur Render et Netlify, avec les garde-fous adaptes a une demo portfolio.

**Infrastructure de deploiement.**

* Deux Dockerfiles multi-stage (`sejour-backend/Dockerfile` et `pastell-mock/Dockerfile`) qui buildent depuis la racine du repo avec `mvn -pl <module> -am`. Permet a Render de construire chaque service independamment.
* `.dockerignore` racine pour reduire le contexte de build envoye au daemon.
* Base PostgreSQL Render free tier, URL interne.
* Frontend principal sur `hotel-montpellier.netlify.app`, deja en place. Dashboard de demo sur `springhotel-pastell-dashboard.netlify.app`, nouveau site Netlify.
* UptimeRobot pour empecher les services Render free tier de s'endormir.

**Rotation des credentials Pastell par derivation HMAC.**

* `PastellCredentialsProvider` cote sejour-backend et `MockCredentialsProvider` cote mock, deux classes symetriques qui derivent un username stable et un password rotatif quotidien depuis un secret maitre partage (`PASTELL_MASTER_SECRET`).
* `RotatingBasicAuthInterceptor` qui recalcule le header `Authorization: Basic` a chaque requete RestClient (le `BasicAuthenticationInterceptor` de Spring fige les valeurs au constructeur).
* `RotatingPasswordEncoder` cote mock qui accepte simultanement le password d'aujourd'hui et celui d'hier (tolerance vingt-quatre heures au passage de minuit UTC).
* Bascule automatique entre mode statique (dev local, defaut) et mode rotatif (prod) via `@ConditionalOnProperty` sur la presence de `pastell.master-secret`.
* Documentation complete dans `CREDENTIALS.md`.

**Garde-fous demo publique.**

* Compte demo `demo@springhotel.fr` mot de passe `Malik971*` role `ROLE_USER`, seede par `SetupDataLoader` au demarrage de maniere idempotente.
* `DemoRateLimitFilter` qui limite dix requetes par IP par fenetre de soixante secondes sur les endpoints d'ecriture sensibles (register, login, reservation, force-poll). Active en prod via `demo.rate-limit.enabled=true`.
* Header `X-Demo-Token` sur `POST /api/admin/pastell/poll`, valeur lue depuis la propriete `demo.admin-token` (variable `DEMO_ADMIN_TOKEN` en prod).
* CORS durci : trois origines explicites (`localhost:*`, frontend principal, dashboard demo) au lieu du wildcard.
* Documentation complete dans `DEMO_PUBLIQUE.md`.

**Observabilite trois couches.**

* Couche 1, Spring Boot Actuator : `/actuator/health` et `/actuator/info` exposes anonymement pour les sondes Render et UptimeRobot. `show-details=never` pour ne pas fuir l'infra.
* Couche 2, endpoint custom `GET /api/admin/pastell/status` (nouvelle methode dans `AdminPastellController`) : retourne un `PastellStatusDTO` avec compteurs par statut de sync, curseur de polling, ping mock. Ajout de `PastellSyncRepository.countBySyncStatus()` pour le comptage efficient cote base.
* Couche 3, page `dashboard/status.html` qui poll l'endpoint custom toutes les cinq secondes en JavaScript vanilla, affichage avec chips colores.
* Documentation complete dans `OBSERVABILITY.md`.

**Schema et donnees prod.**

* `application-prod.properties` reecrit : Flyway active, `ddl-auto=validate`, Pastell active en mode rotatif, Actuator, rate limit.
* Nouveau fichier `pastell-mock/src/main/resources/application-prod.properties` (n'existait pas avant ce lot).
* Migration `V5__seed_demo_hotels.sql` : cinq hotels Montpellier (Le Peyrou, La Comedie, Antigone Plaza, Port Marianne, Les Pres d'Arenes) avec coordonnees GPS realistes, trois chambres par hotel, equipements. Idempotente via `INSERT ... WHERE NOT EXISTS`.

**Modifications applicatives.**

* `PastellProperties.java` : ajout du champ `masterSecret`, methode `isRotatingCredentialsEnabled()`, `validateIfEnabled()` adapte pour accepter le mode rotatif sans username/password.
* `PastellConfig.java` : selection automatique de l'interceptor d'auth (rotatif si master-secret defini, statique sinon).
* `MockSecurityConfig.java` : `MockCredentialsProvider` declare en `@ConditionalOnProperty`, `UserDetailsService` et `PasswordEncoder` choisissent leur source via `ObjectProvider`.
* `SecurityConfig.java` : `DemoRateLimitFilter` ajoute via `addFilterBefore`, `/actuator/health|info` en `permitAll`, origines CORS explicites.
* `AdminPastellController.java` : ajout de `getStatus()` avec ping HTTP du mock via JDK `HttpClient`, `forcePoll()` exige `X-Demo-Token`.
* `SetupDataLoader.java` : seed du compte demo en plus de l'admin existant.


### Outillage de démo

Pour faciliter la présentation du projet et la prise en main par d'autres développeurs, plusieurs outils ont été produits :

* `dashboard/` : interface web statique HTML/CSS/JS qui visualise en temps réel l'état de la réservation côté Sejour et l'état du dossier côté Pastell, avec des boutons pour déclencher des actions et un journal en direct. Code structuré proprement en modules séparés.
* `AdminPastellController` : controller dédié à la démo qui expose des endpoints non-authentifiés pour lire une réservation, lire le PastellSync associé, lire le curseur de polling et forcer un poll manuel.
* Collection Postman complète couvrant les 13 étapes du flux complet.

## Prochaines étapes

* JWT stateless en remplacement de l'authentification session.
* `GlobalExceptionHandler` plus complet pour des erreurs API cohérentes en JSON.
* Migration de `HotelController` vers une couche Service.
* Swagger UI / OpenAPI 3 sur `/swagger-ui`.
* Hook `useAuth` pour un état de connexion global côté frontend.
* Page `MonEspacePage` avec liste des réservations et annulation.
* Tests unitaires et d'intégration plus larges (les starters sont déjà dans `pom.xml`).
* Recherche géospatiale Haversine SQL pour un filtre rayon en km.