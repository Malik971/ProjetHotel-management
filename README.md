# SpringHotel

Application full-stack de réservation hôtelière sur Montpellier, avec une intégration complète du bus d'orchestration Pastell de Libriciel SCOP, une authentification OAuth2 / OpenID Connect via Keycloak, et une connexion sociale Google.

**Stack :** Spring Boot 4 · Java 21 · React 18 · Tailwind CSS v4 · PostgreSQL · Flyway · Keycloak 26 · Docker · Railway · Netlify

---

## Pourquoi ce projet

SpringHotel est mon projet portfolio. Il a été conçu pour démontrer une capacité à intégrer des systèmes hétérogènes, à documenter cette intégration pour plusieurs audiences, et à opérer une application en production.

L'intégration Pastell est au cœur du projet : le bus d'orchestration de Libriciel SCOP y est instrumenté comme il le serait dans un contexte réel de collectivité territoriale. Le mock Pastell que j'ai développé reproduit fidèlement l'API officielle (form-data en entrée, JSON en sortie, HTTP Basic, machine à états du document, journal d'événements), ce qui permet un développement et des tests d'intégration sans dépendre d'une instance Libriciel.

L'authentification OAuth2 / OpenID Connect via Keycloak a été ajoutée parce que c'est la technologie qu'utilise iparapheur v5 de Libriciel, et parce que la fiche de poste visée en fait une exigence explicite.

---

## Démo en ligne

| Service | URL |
|---|---|
| Application principale | [hotel-montpellier.netlify.app](https://hotel-montpellier.netlify.app) |
| Documentation intégration | [hotel-montpellier.netlify.app/admin/docs](https://hotel-montpellier.netlify.app/admin/docs) |
| Dashboard Pastell | [pastell-demo.netlify.app](https://pastell-demo.netlify.app) |

**Compte de démonstration :** `demo@springhotel.fr` / `Malik971*` (lecture et réservation)

**Connexion admin :** disponible via le formulaire ou via Keycloak (bouton dédié sur la page de connexion). La page `/admin/docs` est le point d'entrée recommandé pour comprendre l'architecture et l'intégration.

---

## Architecture déployée

```
hotel-montpellier.netlify.app
        │
        ▼
sejour-backend-production.up.railway.app   ◄──►   keycloak-production-cfd1.up.railway.app
        │                                                   (OAuth2 / OIDC / Google)
        ▼
 PostgreSQL Railway
        │
        ▼
pastell-mock-production.up.railway.app
```

- Trois services Docker sur Railway : `sejour-backend`, `pastell-mock`, `keycloak`.
- Base PostgreSQL Railway avec migrations Flyway automatiques au démarrage.
- Frontend sur Netlify (build Vite), déclenché à chaque push sur `main`.
- UptimeRobot sur `/actuator/health` et `/api/version.php` toutes les 5 minutes.

---

## Ce que l'application fait

**Visiteur** : consultation des hôtels, filtres combinables (prix, catégorie, équipements), carte interactive Leaflet avec marqueurs GPS, détail chambre avec galerie d'images.

**Client connecté** : réservation avec vérification de disponibilité, mail de confirmation, historique des séjours, annulation avec règle de délai.

**Administrateur** : gestion des hôtels, chambres et utilisateurs. Tableau de bord de supervision du bus Pastell en temps réel : compteurs par étape circuit, journal d'événements, relance manuelle sur anomalie.

**Authentification** : trois modes coexistent sur la même page de connexion. JWT maison (comptes locaux), Keycloak PKCE (flow Authorization Code avec code_challenge S256), et connexion directe Google via `kc_idp_hint`. Les deux derniers passent par le même Resource Server Spring Boot via un `CompositeJwtDecoder` qui dispatche selon le claim `iss`.

---

## Architecture du dépôt

```
_WORKSPACE/
├── back-end/
│   ├── pom.xml                 parent Maven multi-module
│   ├── sejour-backend/         Spring Boot principal (port 8080)
│   ├── pastell-mock/           mock Pastell fidele a l'API reelle (port 8090)
│   └── keycloak/
│       ├── Dockerfile          image Keycloak avec realm pre-importe
│       └── realm-export.json   realm springhotel (roles, scopes, client PKCE)
│
└── front-end/                  React 18 + Vite + Tailwind CSS v4 (port 5173)
```

---

## L'intégration Pastell

Développée par lots successifs, chacun ciblant une dimension précise de l'interopérabilité.

**Lot 1** : architecture multi-module Maven, séparation stricte Sejour / mock Pastell.

**Lot 2** : mock Pastell avec endpoints `/api/v2/document` et `/api/v2/journal`, machine à états complète, authentification HTTP Basic, journal d'événements.

**Lot 3** : synchronisation montante. Chaque création de réservation côté Sejour déclenche un événement `ReservationCreatedEvent` qui crée un dossier côté Pastell. Idempotence garantie.

**Lot 4** : retry à deux niveaux. Premier niveau : tentative immédiate sur échec. Deuxième niveau : scheduler de reprise différée pour les dossiers en anomalie. Statuts `EN_RETRY` et `EN_ERREUR` visibles dans l'interface admin.

**Lot 5** : synchronisation descendante. Sejour interroge `GET /api/v2/journal?since_id_j=N` toutes les 30 secondes. Un curseur persisté en base garantit qu'aucune entrée n'est traitée deux fois.

**K1-K5** : intégration Keycloak. Infrastructure Docker (K1), Resource Server OAuth2 en coexistence avec le JWT maison via `CompositeJwtDecoder` (K2), JIT provisioning des utilisateurs Keycloak (K3), flow Authorization Code PKCE côté frontend en JavaScript natif sans dépendance externe (K4), documentation technique de l'intégration dans l'interface admin (K5).

La documentation de chaque lot est disponible dans les fichiers `RETRY.md` et `POLLING.md` dans `sejour-backend`, et dans la page `/admin/docs` de l'application déployée.

---

## Développement local

### Prérequis

Java 21, Node 18+, Maven 3.9+, Docker Desktop.

### Démarrage rapide

```bash
# 1. Infrastructure (PostgreSQL + Keycloak)
cd back-end
./run-local.sh infra

# Attendre le message "Import finished successfully" dans les logs Keycloak
docker compose logs -f keycloak

# 2. Mock Pastell (terminal séparé)
./run-local.sh mock

# 3. Backend principal (terminal séparé)
./run-local.sh backend

# 4. Frontend (terminal séparé)
cd front-end
npm install
npm run dev
```

L'application est disponible sur `http://localhost:5173`.

### Variables d'environnement

Copie les fichiers d'exemple :

```bash
cp _env.example _env
cp back-end/sejour-backend/src/main/resources/application-local.properties.example \
   back-end/sejour-backend/src/main/resources/application-local.properties
```

Les variables critiques à renseigner sont `JWT_SECRET`, `PASTELL_MASTER_SECRET`, et les credentials PostgreSQL. Les fichiers `.env` et `application-local.properties` ne sont pas commités.

### Keycloak local

Le realm `springhotel` est importé automatiquement au premier démarrage depuis `back-end/keycloak/realm-export.json`.

Comptes de démo Keycloak : `admin-demo` / `Admin1234!` et `user-demo` / `User1234!`.

Console d'administration : `http://localhost:8180/admin`.

Pour réinitialiser le realm (après modification de `realm-export.json`) :

```bash
cd back-end
docker compose stop keycloak
docker compose rm -f keycloak
docker volume rm back-end_hotel_keycloak_data
docker compose up -d keycloak
```

---

## Conventions de développement

**Backend** : un Controller ne parle jamais directement à un Repository. Toute logique passe par une couche Service. Les variables métier sont nommées en français (`prixMoyenNuit`, `dateDebut`, `etapeCircuit`) parce que ce sont des concepts du domaine Pastell. Les utilitaires techniques restent en anglais.

**Frontend** : les appels API ne se font jamais directement dans un composant. Ils passent par des fonctions dans `services/` ou des hooks dans `hooks/`.

**Commits** : conventional commits avec portée explicite. Exemples : `feat(pastell):`, `fix(auth):`, `feat(docs):`, `chore(infra):`.

**Vocabulaire Libriciel** appliqué dans le code et la documentation : `dossier` (pas `sync`), `etape circuit` (pas `status`), `anomalie` (pas `error`), `relancer` (pas `retry`), `bus d'orchestration` (pas `integration`).

---

## État du projet

**Fonctionnel en production :**

- Recherche d'hôtels avec filtres combinables et carte Leaflet
- Système de réservation complet avec règles d'annulation
- Emails de confirmation via spring-boot-mail
- Espace client et panel admin
- Intégration Pastell bidirectionnelle (5 lots : création montante, retry, polling descendant)
- Authentification OAuth2 / OpenID Connect via Keycloak (JWT maison en coexistence)
- Connexion Google via Keycloak (kc_idp_hint)
- Documentation technique d'intégration accessible sur `/admin/docs`
- Environ 200 tests unitaires et d'intégration
- Déploiement Docker sur Railway avec healthchecks

**Non implémenté (identifié et assumé) :**

- Paiement réel
- Email transactionnel (confirmation par Pastell)
- OpenAPI / Swagger UI
- Recherche géospatiale par rayon

---

## Auteur

[Malik Ibo](https://github.com/Malik971) - Concepteur Développeur d'Applications (Dawan, Montpellier)

Le projet a été initié dans le cadre d'une formation en binôme. Les lots d'intégration Pastell, l'infrastructure Keycloak et tous les développements depuis janvier 2026 sont l'œuvre de Malik Ibo seul.