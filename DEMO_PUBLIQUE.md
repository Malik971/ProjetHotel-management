# Demo publique, garde-fous et limites

Ce document detaille la posture de securite adoptee pour la mise en ligne de SpringHotel dans un contexte de demonstration portfolio. La cible est claire : presenter le projet en mode "interactif, on peut cliquer partout" a des recruteurs et a des techs, sans laisser de portes grandes ouvertes pour autant.

Plusieurs choix sont des compromis assumes entre experience visiteur et durcissement. Ils sont documentes explicitement pour ne pas etre confondus avec une approche production.

## Compte demo

L'application expose un compte fixe pour les visiteurs.

* Email : `demo@springhotel.fr`
* Mot de passe : `Malik971*`
* Role : `ROLE_USER` (uniquement de la lecture et la creation de reservations, pas d'admin)

Ce compte est cree automatiquement au demarrage par `SetupDataLoader`, de maniere idempotente. Si un admin change le mot de passe en base, le seeding ne le restaurera pas.

Un visiteur qui veut faire le tour des fonctionnalites lit le mot de passe sur la page d'accueil, se connecte, simule une reservation, voit le polling reagir.

## Compte employe de demo

Pour faire visiter l'espace d'administration sans risque, un second compte fixe est expose.

* Email : `employe@springhotel.fr`
* Mot de passe : `Employe971*`
* Roles : `ROLE_EMPLOYE` + `ROLE_USER`

Ce compte donne acces a tout l'espace admin (tableau de bord Pastell, gestion des hotels, chambres et utilisateurs, relance manuelle du bus) en lecture, creation et modification, mais PAS en suppression. Les boutons supprimer sont masques cote frontend, et le backend refuse de toute facon tout `DELETE` provenant d'un non-admin (voir `SecurityConfig`). Un visiteur peut donc tout explorer et tout modifier sans pouvoir casser le jeu de donnees de demo.

Comme le compte demo client, il est cree au demarrage par `SetupDataLoader` de maniere idempotente. Il existe aussi en version Keycloak (`employe-demo` / `Employe1234!`) pour la connexion via le bouton Keycloak.

## Token administrateur sur les operations destructives

Le endpoint `POST /api/admin/pastell/poll`, qui force un tick de polling Pastell, est protege par un header :

```
X-Demo-Token: <valeur generee a l'install>
```

Cette valeur est definie cote backend par la variable d'environnement `DEMO_ADMIN_TOKEN`. Le dashboard de demo connait la meme valeur via `dashboard/config.js`.

**Limite assumee :** le token est inscrit dans le JavaScript du dashboard, il est donc accessible a quiconque inspecte le bundle. Ce n'est pas une defense contre un attaquant determine. C'est une defense contre :

* Les bots qui taperaient sur tous les `/api/admin/*` qu'ils trouvent.
* Les visiteurs curieux qui essaieraient un `curl` direct sans avoir lu le code.

Cela suffit pour eviter le bruit. Si quelqu'un de motive force un poll, le pire qu'il puisse faire est de consommer un peu de CPU sur Render. Pas de perte de donnees, pas de fuite d'info.

## Rate limit

Un filtre Spring intercepte les requetes sur les endpoints sensibles avant l'authentification. Limite : dix requetes par IP par fenetre de soixante secondes.

Endpoints proteges :

* `POST /api/v1/register` (creation de compte)
* `POST /api/v1/login` (authentification)
* `POST /api/reservations` (creation de reservation)
* `POST /api/admin/pastell/poll` (force-poll)

Au-dela, le serveur repond `429 Too Many Requests` avec un header `Retry-After: 60`.

Ce filtre est code maison, sans dependance externe (pas de Bucket4j, pas de Redis), parce que pour un portfolio mono-instance free tier c'est largement suffisant et ca evite de gonfler le projet d'une couche d'infra qui ne sert qu'a une chose.

**Limite assumee :** un attaquant peut faire tourner les IPs. Mais a ce stade-la, il pourrait aussi simplement DDOS le free tier Render, ce qui n'est pas notre probleme a defendre.

## CORS

Trois origines autorisees explicitement :

* `http://localhost:*` (developpement)
* `https://hotel-montpellier.netlify.app` (frontend principal)
* `https://springhotel-pastell-dashboard.netlify.app` (dashboard de demo)

Toute autre origine est rejetee par le navigateur. C'est un durcissement par rapport au `*` qui existait avant le Lot 6.

## Routes admin

`/api/admin/**` reste en `permitAll` au niveau Spring Security pour que le dashboard de demo puisse l'appeler depuis un navigateur. La protection effective est :

* CORS qui limite les origines (cf. plus haut).
* Le rate limit sur `/api/admin/pastell/poll`.
* Le `X-Demo-Token` sur `/api/admin/pastell/poll`.

Pour une vraie prod, ces routes seraient en `hasRole("ADMIN")` et la demo aurait besoin d'un JWT admin. Pour ce portfolio, le triptyque CORS + rate limit + token public suffit.

## Donnees

La base PostgreSQL Render n'est jamais exposee directement. Seul sejour-backend la lit, via l'URL interne Render qui n'est joignable qu'entre services du meme compte.

Pas de donnees personnelles reelles. Les utilisateurs qui s'inscrivent (registration libre, le frontend l'autorise) creent des comptes consultables uniquement par eux. Aucune validation d'email, aucun envoi de mail (sauf si `MAIL_USERNAME` et `MAIL_PASSWORD` sont configures).

## Ce qui n'est PAS securise et pourquoi c'est OK ici

* **Pas de JWT, pas de refresh token :** les sessions sont stateless via le contexte Spring Security par requete. Pas adapte a une vraie app, mais bien suffisant pour une demo.
* **Pas de HTTPS force au niveau backend :** Render et Netlify gerent HTTPS au niveau de l'edge, l'app derriere ne s'en occupe pas.
* **Le compte admin `test@test.com` reste seede :** son mot de passe `test123` est en clair dans le code. Un attaquant qui lit le code source peut se loguer admin. Pour une demo c'est volontairement laisse pour pouvoir tester les fonctionnalites admin localement et a distance.

Ces choix sont coherents avec l'objectif : un portfolio interactif, pas une plateforme commerciale. Quand le projet sera utilise en vrai, ce document sera reecrit.
