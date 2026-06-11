# Observabilite, trois couches

Le projet expose son etat de fonctionnement via trois couches superposees, du plus generique au plus specifique. Chaque couche repond a un cas d'usage different et reste utilisable independamment des autres.

## Couche 1, Spring Boot Actuator

Couche par defaut pour les sondes infrastructure (Railway healthcheck, UptimeRobot, Kubernetes liveness/readiness si on y passait un jour).

Endpoints actifs en prod :

* `GET /actuator/health` : etat synthetique de l'application (`UP` ou `DOWN`). Anonyme.
* `GET /actuator/info` : metadonnees statiques (`info.app.name`, `info.app.version`, `info.app.lot`). Anonyme.
* `GET /actuator/metrics` : metriques internes (JVM, HTTP, datasource). Necessite une authentification, pas expose en demo.

Configuration dans `application-prod.properties` :

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=never
```

`show-details=never` empeche d'exposer les details du healthcheck (URL de base, version de Postgres, etc.) qui pourraient renseigner un attaquant sur l'infrastructure.

Utilise par :

* Railway, qui ping `/actuator/health` toutes les minutes pour decider si le service est sain.
* UptimeRobot, qui ping `/actuator/health` toutes les cinq minutes pour empecher le free tier de s'endormir.

## Couche 2, endpoint custom `/api/admin/pastell/status`

Couche metier qui agrege les informations utiles a un humain pour comprendre rapidement l'etat de l'integration Pastell.

Endpoint : `GET /api/admin/pastell/status`, public (pas de token requis), retourne un JSON :

```json
{
  "generatedAt": "2026-05-11T14:32:01.245",
  "pastellEnabled": true,
  "lastProcessedIdJ": 247,
  "lastPolledAt": "2026-05-11T14:31:43.102",
  "syncCountOk": 23,
  "syncCountPending": 0,
  "syncCountEnRetry": 1,
  "syncCountEnErreur": 0,
  "syncCountDivergence": 0,
  "reservationCount": 31,
  "mockHealth": {
    "reachable": true,
    "responseTimeMs": 124,
    "errorMessage": null
  }
}
```

Compteurs `syncCount*` issus de `PastellSyncRepository.countBySyncStatus()`, qui execute un `SELECT COUNT(*)` cote base sans charger d'entites en memoire.

`mockHealth` issu d'un ping HTTP `GET /api/version.php` sur le mock, avec un timeout de deux secondes pour ne pas bloquer la reponse en cas de mock indisponible. Le ping utilise le JDK `HttpClient` directement, sans dependre de `PastellConfig`, pour que ce endpoint reste accessible meme si l'integration Pastell est mal configuree ou desactivee.

Implementation : `AdminPastellController.getStatus()`.

## Couche 3, page HTML `status.html`

Couche presentation. Page statique servie sur `https://springhotel-pastell-dashboard.netlify.app/status.html`.

Polle la couche 2 toutes les cinq secondes en JavaScript pur, met a jour un tableau de bord en SPA-light, sans framework.

Affichage :

* Trois cartes synthese en haut : backend, mock, dernier polling.
* Cinq compteurs colores en grille : OK, PENDING, EN_RETRY, EN_ERREUR, DIVERGENCE.
* Une ligne volume : nombre total de reservations.
* Un bloc `<details>` repliable avec le JSON brut.
* Footer avec le timer de rafraichissement et l'URL source.

Code chips de couleur, repris du frontend principal :

| Statut       | Couleur de fond | Texte           |
|--------------|-----------------|-----------------|
| OK           | vert pale       | vert fonce      |
| PENDING      | bleu pale       | bleu fonce      |
| EN_RETRY     | ambre pale      | ambre fonce     |
| EN_ERREUR    | rouge pale      | rouge fonce     |
| DIVERGENCE   | rose pale       | magenta fonce   |

Utile pour :

* Verifier en un coup d'oeil pendant une demo que les compteurs bougent quand on cree une reservation.
* Detecter une derive (EN_RETRY qui s'empile, ou DIVERGENCE qui apparait) sans avoir a se logguer en SSH.
* Donner un lien permanent a partager dans un CV ou un dossier de candidature.

## Pourquoi pas Prometheus + Grafana

Decision prise au demarrage du Lot 6 : pas de couche 4 Prometheus.

* Prometheus exige un serveur de scraping en plus du backend a monitorer. Sur Railway, c'est un troisieme service a maintenir.
* Grafana en mode SaaS gratuit existe (Grafana Cloud), mais ca rajoute une dependance externe pour zero benefice cote demo.
* Pour un portfolio, montrer un dashboard custom HTML est plus parlant qu'un Prometheus genere automatiquement : ca demontre que le candidat sait choisir des indicateurs metier, pas juste plugger un truc tout fait.

Si le projet devait vraiment scaler, on rajouterait sans probleme `management.endpoints.web.exposure.include=health,info,metrics,prometheus`, un Prometheus en sidecar, et un Grafana point dessus. La porte reste ouverte mais on ne paye pas la dette tant qu'elle n'est pas necessaire.

## Cycle de vie d'un evenement, vue d'observabilite

Pour illustrer comment les trois couches se completent, exemple d'une reservation qui produit un cycle complet :

```
1. Client cree une reservation
   ───────────────────────────
   - Couche 1 (Actuator) : aucune trace specifique
   - Couche 2 (status)   : reservationCount passe de N a N+1, syncCountPending de 0 a 1
   - Couche 3 (page)     : compteur PENDING passe a 1 (chip bleu)

2. ReservationCreatedListener appelle Pastell, succes
   ────────────────────────────────────────────────
   - Couche 1 : aucune trace
   - Couche 2 : syncCountPending repasse a 0, syncCountOk passe de M a M+1
   - Couche 3 : PENDING repasse a 0, OK incremente, chip vert

3. Le polling tourne 30s plus tard et marque l'event traite
   ────────────────────────────────────────────────────────
   - Couche 1 : aucune trace
   - Couche 2 : lastProcessedIdJ passe de I a I+1, lastPolledAt mis a jour
   - Couche 3 : "dernier polling" passe a "il y a quelques secondes"
```

Si Pastell est down a l'etape 2 :

```
- Couche 2 : syncCountEnRetry passe a 1, syncCountPending toujours 0
- Couche 3 : compteur EN_RETRY a 1 (chip ambre)
```

Si Pastell remonte trente minutes plus tard, le scheduler du Lot 4 retraite :

```
- Couche 2 : syncCountEnRetry repasse a 0, syncCountOk incremente
- Couche 3 : tout repasse au vert
```

Si Pastell envoie une action incoherente avec l'etat Sejour (cas de divergence du Lot 5) :

```
- Couche 2 : syncCountDivergence passe a 1
- Couche 3 : compteur DIVERGENCE a 1 (chip rose vif), demande arbitrage humain
```

Trois couches qui se completent : l'infra ping la couche 1, l'integrateur lit la couche 2, le decideur visite la couche 3.
