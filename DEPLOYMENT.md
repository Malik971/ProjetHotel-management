# Guide de mise en ligne, SpringHotel + Pastell mock

Ce document est le pas a pas operationnel pour redeployer l'ensemble du projet sur Render et Netlify, en partant d'un compte vide. Compter trente minutes a une heure pour la premiere fois.

L'architecture cible est volontairement gratuite et sans frais cachés : Render pour les deux services Java et la base PostgreSQL, Netlify pour les deux frontends statiques, UptimeRobot pour garder les services Render free tier eveilles.

## Vue d'ensemble

```
                    ┌───────────────────────────────────────────┐
                    │                                           │
   utilisateurs ──► hotel-montpellier.netlify.app  (React app)  │
                    │           │                               │
                    │           ▼                               │
                    │   springhotel-backend.onrender.com        │
                    │           │                               │
                    │           ▼                               │
                    │   PostgreSQL Render                       │
                    │           │                               │
                    │           ▼                               │
                    │   springhotel-pastell-mock.onrender.com   │
                    │                                           │
   recruteurs   ──► springhotel-pastell-dashboard.netlify.app   │
                    │                                           │
                    └───────────────────────────────────────────┘
```

## Pré-requis

* Un compte GitHub avec le repo SpringHotel pousse.
* Un compte Render (gratuit, signup avec GitHub).
* Un compte Netlify (gratuit, signup avec GitHub).
* Un compte UptimeRobot (gratuit, jusqu'a 50 monitors).
* Un terminal local avec `openssl` (pour generer le secret partage).

## Étape 1, generer les secrets

Avant tout, generer le secret maitre qui sera partage entre sejour-backend et pastell-mock.

```bash
openssl rand -hex 32
# Exemple de sortie :
# a3f29c8f1d4b7e6a8c5d9b2e4f1a7d6b8c3e5a9f2d4c7b6a8e1f3d5c7b9a2e4f
```

Garder cette valeur de cote, on en aura besoin trois fois (une fois sur chaque service Render).

Genere aussi un token administrateur pour la demo :

```bash
openssl rand -hex 16
# Exemple : 7c8f1d4b9e6a8c5d3b2e4f1a7d6b8c3e
```

## Étape 2, creer la base PostgreSQL sur Render

1. Aller sur `dashboard.render.com`, cliquer New, puis PostgreSQL.
2. Name : `springhotel-db`. Region : Frankfurt (proximite France). Plan : Free.
3. Cliquer Create Database. Render genere les credentials.
4. Une fois la base creee, copier les valeurs suivantes (onglet Info) :
   * Internal Database URL
   * Internal Database Username
   * Internal Database Password
   * On utilisera les URLs internes : free tier, mais elles fonctionnent entre services Render de la meme region.

## Étape 3, deployer pastell-mock sur Render

On deploie pastell-mock en premier car sejour-backend en depend pour demarrer proprement.

1. Render dashboard, New, Web Service.
2. Connecter le repo GitHub SpringHotel.
3. Configuration :
   * Name : `springhotel-pastell-mock`
   * Region : Frankfurt
   * Branch : `main` (ou la branche de release du moment)
   * Root Directory : laisser vide
   * Runtime : Docker
   * Dockerfile Path : `pastell-mock/Dockerfile`
   * Docker Build Context Directory : `.`
   * Plan : Free
4. Variables d'environnement (cliquer Add Environment Variable) :
   * `SPRING_PROFILES_ACTIVE` = `prod`
   * `PASTELL_MASTER_SECRET` = la valeur generee a l'etape 1
   * `PORT` = `8090` (Render injecte aussi automatiquement, mais on force pour clarte)
5. Cliquer Create Web Service.
6. Attendre le premier build (cinq a dix minutes la premiere fois, le temps de pull Maven 3.9 et Temurin 21).
7. Quand le service est UP, noter son URL publique. Format : `https://springhotel-pastell-mock.onrender.com`.

Verification : ouvrir `https://springhotel-pastell-mock.onrender.com/actuator/health` dans le navigateur, on doit voir `{"status":"UP"}`.

## Étape 4, deployer sejour-backend sur Render

1. Render dashboard, New, Web Service.
2. Memes etapes que pour le mock, avec :
   * Name : `springhotel-backend`
   * Dockerfile Path : `sejour-backend/Dockerfile`
   * Docker Build Context Directory : `.`
3. Variables d'environnement :
   * `SPRING_PROFILES_ACTIVE` = `prod`
   * `DATABASE_URL` = l'Internal Database URL de l'etape 2
   * `DATABASE_USERNAME` = l'Internal Database Username
   * `DATABASE_PASSWORD` = l'Internal Database Password
   * `PASTELL_BASE_URL` = l'URL publique du mock obtenue a l'etape 3
   * `PASTELL_MASTER_SECRET` = la meme valeur qu'a l'etape 3, identique au caractere pres
   * `DEMO_ADMIN_TOKEN` = le token genere a l'etape 1 (deuxieme commande openssl)
   * `MAIL_USERNAME` = optionnel, votre adresse Gmail pour l'envoi de mails
   * `MAIL_PASSWORD` = optionnel, votre mot de passe d'application Gmail
   * `PORT` = `8080`
4. Create Web Service.
5. Attendre le build.

Verification : `https://springhotel-backend.onrender.com/actuator/health` doit retourner `{"status":"UP"}`. Tester aussi `https://springhotel-backend.onrender.com/api/hotels` qui doit retourner un tableau des cinq hotels Montpellier seedes par la migration V5.

## Étape 5, deployer le frontend React (deja existant)

Le site `hotel-montpellier.netlify.app` est deja deploye. S'assurer que la variable `VITE_API_URL` pointe sur `https://springhotel-backend.onrender.com` dans le panneau Netlify, sinon le faire et redeployer.

## Étape 6, deployer le dashboard Pastell sur Netlify

1. Netlify dashboard, Add new site, Import an existing project.
2. Connecter le repo SpringHotel, choisir la branche `main`.
3. Configuration :
   * Base directory : `dashboard`
   * Build command : (laisser vide)
   * Publish directory : `dashboard`
4. Variables d'environnement (Site settings, Environment variables) :
   * Ces variables seront lues par `dashboard/config.js`, mais comme il n'y a pas de build step, il faut soit les inliner dans config.js avant push, soit utiliser un post-build hook. Pour la version la plus simple : editer `dashboard/config.js` directement et commiter avec les bonnes valeurs avant de pousser sur Netlify.
   * Valeurs a mettre :
     * `backendUrl` : `https://springhotel-backend.onrender.com`
     * `mockUrl` : `https://springhotel-pastell-mock.onrender.com`
     * `demoAdminToken` : la meme valeur que `DEMO_ADMIN_TOKEN` cote Render
5. Deploy site. Netlify genere une URL aleatoire que tu peux renommer en `springhotel-pastell-dashboard.netlify.app` dans Domain settings.

Verification : ouvrir `https://springhotel-pastell-dashboard.netlify.app/status.html`, on doit voir les chips de status passer en vert au bout de quelques secondes.

## Étape 7, configurer UptimeRobot

Sans pings reguliers, Render free tier endort les services au bout de quinze minutes d'inactivite, ce qui produit un cold start de cinquante secondes a la prochaine requete. UptimeRobot evite ca.

1. `uptimerobot.com`, signup.
2. Add New Monitor, type HTTP(s) :
   * Friendly Name : `springhotel-backend`
   * URL : `https://springhotel-backend.onrender.com/actuator/health`
   * Monitoring Interval : 5 minutes
3. Add New Monitor, deuxieme :
   * Friendly Name : `springhotel-pastell-mock`
   * URL : `https://springhotel-pastell-mock.onrender.com/actuator/health`
   * Monitoring Interval : 5 minutes
4. Save les deux.

Les deux services restent maintenant chauds en permanence.

## Validation finale

* `https://hotel-montpellier.netlify.app` : le site React principal s'affiche, les cinq hotels Montpellier apparaissent sur la carte et en liste.
* Se connecter avec `demo@springhotel.fr` / `Malik971*`. La connexion doit reussir.
* Faire une reservation. Apres dix a quinze secondes, voir le compteur OK augmenter sur la page status du dashboard.
* `https://springhotel-pastell-dashboard.netlify.app/status.html` : tous les chips sont verts, le compteur de reservations a augmente.

## Redeploiement

Tout est sur main, donc un push declenche les rebuilds automatiquement sur Render et Netlify. Les deux services Render se rebuildent en parallele. Compter cinq a dix minutes pour avoir l'ensemble en ligne apres un push.

## Que faire si ca casse

* Build Render qui echoue : consulter les logs sur la page Logs du service Render. Les causes classiques sont une variable d'env manquante, une migration Flyway qui ne passe pas (regarder la sortie de Flyway dans les logs).
* `502 Bad Gateway` sur Render : le service est en cold start, attendre cinquante secondes et reessayer.
* `401 Unauthorized` sur les appels Pastell entre sejour et mock : verifier que `PASTELL_MASTER_SECRET` est strictement identique sur les deux services Render. Une espace en trop suffit a casser l'auth.
* Status page qui affiche des chips rouges : verifier les variables d'env du dashboard Netlify (`backendUrl`, `mockUrl`).
