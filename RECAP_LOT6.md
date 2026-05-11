# Recap Lot 6, mise en ligne et observabilite

Ce lot ferme la phase de developpement de l'integration Pastell en rendant le projet accessible publiquement. Objectif : qu'un visiteur (recruteur, candidat developpeur, technologue Libriciel) puisse en deux clics se faire une idee complete de ce qui a ete construit, sans avoir besoin de cloner et de lancer le projet en local.

## Ce qui est livre

* Deux services Java dockerises et deployes sur Render, gratuits, qui restent eveilles grace a UptimeRobot.
* Une base PostgreSQL Render avec cinq hotels Montpellier seeds par migration Flyway et un compte demo pre-cree.
* Le frontend React principal sur Netlify, deja en place, pointant vers le backend Render.
* Un dashboard de demo Pastell sur Netlify avec une page de status temps reel.
* Un mecanisme de rotation des credentials Pastell par derivation HMAC, qui evite de stocker un mot de passe partage en clair.
* Trois couches d'observabilite (Actuator, endpoint custom, page HTML) avec leur documentation respective.

## Decisions techniques notables

**Architecture de deploiement : deux services Render distincts, deux Dockerfiles multi-stage.**

sejour-backend et pastell-mock ont chacun leur Dockerfile, place dans leur module respectif, qui builde depuis la racine du repo (`-pl module -am`). Cette separation reflete l'organisation Maven multi-module et permet de redeployer un service sans rebuilder l'autre.

**Credentials Pastell par derivation HMAC.**

Plutot que de stocker `PASTELL_USERNAME` et `PASTELL_PASSWORD` dans Render, on stocke un seul `PASTELL_MASTER_SECRET` partage entre les deux services. Le username et le password sont derives a la volee :

```
username = "sejour-" + hex(HMAC-SHA256(secret, "username"))[0..16]      (stable)
password = base64url(HMAC-SHA256(secret, "password:" + UTC_DATE))[0..32] (quotidien)
```

Le mock accepte simultanement le password d'aujourd'hui et celui d'hier pour tolerer le passage de minuit UTC. Voir `CREDENTIALS.md`.

**Compte demo public.**

Email `demo@springhotel.fr`, mot de passe `Malik971*`, role `ROLE_USER`. Affiche en clair sur la page d'accueil du dashboard, cree par `SetupDataLoader` au demarrage si absent.

**Rate limit maison.**

Pas de Bucket4j, pas de Redis. Une `ConcurrentHashMap<String, Bucket>` par IP, dix requetes par fenetre de soixante secondes sur les endpoints d'ecriture sensibles. Code dans `DemoRateLimitFilter`. Active par propriete `demo.rate-limit.enabled=true` en prod, off en dev local.

**Token sur les operations destructives.**

`POST /api/admin/pastell/poll` exige un header `X-Demo-Token`. Le dashboard de demo le connait via son `config.js`. Pas une defense crypto, mais un filtre suffisant pour bloquer les bots aveugles. Documente comme un compromis explicite dans `DEMO_PUBLIQUE.md`.

**Trois couches d'observabilite.**

* Spring Boot Actuator pour les sondes infra : `/actuator/health`, `/actuator/info`.
* Endpoint custom `GET /api/admin/pastell/status` qui agrege les compteurs metier (PastellSync par statut, curseur de polling, ping mock).
* Page `status.html` sur le dashboard qui poll cet endpoint toutes les cinq secondes et l'affiche en chips colores.

Pas de Prometheus, pas de Grafana. Decision documentee dans `OBSERVABILITY.md` : pour un portfolio, montrer un dashboard custom est plus parlant qu'un Prometheus generique.

## Fichiers livres

```
sejour-backend/
├── Dockerfile                                                      [nouveau]
├── src/main/java/com/example/springhotel/
│   ├── integration/pastell/
│   │   ├── security/
│   │   │   ├── PastellCredentialsProvider.java                     [nouveau]
│   │   │   └── RotatingBasicAuthInterceptor.java                   [nouveau]
│   │   ├── config/
│   │   │   ├── PastellProperties.java                              [modifie]
│   │   │   └── PastellConfig.java                                  [modifie]
│   │   └── repository/PastellSyncRepository.java                   [modifie]
│   ├── filter/DemoRateLimitFilter.java                             [nouveau]
│   ├── dto/PastellStatusDTO.java                                   [nouveau]
│   ├── configuration/SecurityConfig.java                           [modifie]
│   ├── controller/AdminPastellController.java                      [modifie]
│   └── component/SetupDataLoader.java                              [modifie]
└── src/main/resources/
    ├── application-prod.properties                                 [reecrit]
    └── db/migration/V5__seed_demo_hotels.sql                       [nouveau]

pastell-mock/
├── Dockerfile                                                      [nouveau]
├── src/main/java/com/example/pastellmock/
│   ├── security/
│   │   ├── MockCredentialsProvider.java                            [nouveau]
│   │   └── RotatingPasswordEncoder.java                            [nouveau]
│   └── config/MockSecurityConfig.java                              [modifie]
└── src/main/resources/application-prod.properties                  [nouveau]

dashboard/
├── netlify.toml                                                    [nouveau]
├── config.js                                                       [nouveau]
├── status.html                                                     [nouveau]
├── status.css                                                      [nouveau]
└── status.js                                                       [nouveau]

racine/
├── .dockerignore                                                   [nouveau]
├── DEPLOYMENT.md                                                   [nouveau]
├── DEMO_PUBLIQUE.md                                                [nouveau]
├── CREDENTIALS.md                                                  [nouveau]
├── OBSERVABILITY.md                                                [nouveau]
├── RECAP_LOT6.md                                                   [nouveau]
└── Changelog.md                                                    [modifie]
```

## Ce qui n'est pas fait dans ce lot

* **Pas de nouveaux tests.** Le perimetre du Lot 6 est la mise en ligne, pas l'augmentation de la couverture. Les composants ajoutes (DemoRateLimitFilter, PastellCredentialsProvider, RotatingPasswordEncoder, endpoint status) sont testables et seront couverts en Lot 7 si besoin. Les 133 tests existants restent verts.
* **Pas de migration vers HikariCP-pooling tuning.** Les defauts Spring Boot suffisent pour la demo.
* **Pas de cache HTTP applicatif.** L'endpoint status pourrait gagner a etre cache cinq secondes pour eviter de spammer le mock, mais le cout actuel est negligeable.

## Verification post-deploiement

* `https://hotel-montpellier.netlify.app` : carte avec cinq hotels, login `demo@springhotel.fr` / `Malik971*`.
* `https://springhotel-pastell-dashboard.netlify.app/status.html` : tous chips verts au bout de quelques secondes.
* Creation d'une reservation depuis l'app -> compteur OK incremente sur la page status dans la minute.

## Ce qui est ouvert pour les prochains lots

* Lot 7 : tests sur les composants ajoutes au Lot 6, JWT stateless, hardening admin.
* Sprint 2 (back-end) : recherche geospatiale Haversine SQL, filtres avances.
* Sprint 3 (portfolio premium) : composants frontend interactifs supplementaires, e2e Playwright.
