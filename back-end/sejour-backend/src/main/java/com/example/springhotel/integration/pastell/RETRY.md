# Resilience de l'integration Pastell (Lot 4)

## Pourquoi deux niveaux de retry ?

Pastell peut echouer pour deux raisons fondamentalement differentes :

1. **Pannes courtes** (quelques secondes) : un hoquet reseau, une 503 fugace,
   un GC un peu long cote Pastell. La bonne reponse est de re-essayer
   tout de suite, dans la meme transaction d'appel.

2. **Pannes longues** (plusieurs minutes a plusieurs heures) : Pastell est
   en maintenance, son serveur est down, sa base est inaccessible. Re-essayer
   tout de suite ne sert a rien : il faut persister "je dois re-essayer
   plus tard" et avoir un mecanisme qui reprend regulierement.

## Architecture

Niveau 1 absorbe le bruit. Niveau 2 absorbe les vraies pannes.

## Politique de decision

Le code HTTP determine si un echec est retryable ou non.

| Code         | Retryable | Raison                                        |
|--------------|-----------|-----------------------------------------------|
| 5xx          | oui       | Panne cote Pastell, peut se resoudre          |
| 408          | oui       | Request timeout serveur                       |
| 429          | oui       | Rate limit, attendre puis re-essayer          |
| NETWORK      | oui       | Timeout, DNS, connexion refusee               |
| 400/401/403  | non       | Bug ou config cote Sejour                     |
| 404          | non       | Ressource introuvable, pas de magie possible  |
| 409          | non       | Conflit metier, retry ne reglera pas          |

Une erreur non-retryable bascule **directement** en `EN_ERREUR`,
sans attendre que le quota total de tentatives soit epuise. Le sync
n'est plus jamais retraite par le scheduler.

## Configuration

Tout est dans `pastell.retry.*` (voir `PastellProperties.Retry`).
Defauts raisonnables, surchargeables par variables d'env :

```properties
pastell.retry.max-attempts-immediate=3
pastell.retry.initial-delay-ms=200
pastell.retry.multiplier=2.0
pastell.retry.max-delay-ms=2000
pastell.retry.scheduler-enabled=true
pastell.retry.scheduler-interval-ms=300000
pastell.retry.scheduler-batch-size=20
pastell.retry.max-tentatives-total=10
```

## Ce qu'on ferait pour aller plus loin (hors scope Lot 4)

- **Circuit breaker** (Resilience4j) : ouvrir le circuit au-dela d'un seuil
  d'echecs pour arreter de taper sur Pastell. Particulierement utile en
  multi-tenant ou un Pastell HS noierait les autres.
- **Pacing dans le batch** : delai entre deux appels du meme batch pour
  ne pas declencher un effet "thundering herd" au retour de Pastell.
- **Dead Letter Queue** : table dediee pour les EN_ERREUR avec interface
  admin permettant un retraitement manuel.
- **Metriques Micrometer** : compteur de retries niveau 1, gauge des
  EN_RETRY en attente, timer des appels Pastell.
- **Jitter** : ajouter une randomisation au backoff pour eviter que tous
  les clients Pastell repartent au meme instant apres une panne.

## Cycles de vie typiques

**Cas nominal** : `PENDING -> OK` en quelques ms.

**Cas hoquet reseau** : `PENDING -> OK` en ~1 seconde
(2 retries niveau 1 reussissent au troisieme essai).

**Cas panne longue resolue** :
`PENDING -> EN_RETRY (tentatives=3) -> EN_RETRY (tentatives=6) -> OK (tentatives=7)`
La premiere passe via le listener (3 tentatives niveau 1), puis le scheduler
prend le relais a chaque passe.

**Cas mauvaise config** :
`PENDING -> EN_ERREUR` directement, sans retry inutile (401 detecte immediatement).

**Cas panne irrecuperable** :
`PENDING -> EN_RETRY -> EN_RETRY -> ... -> EN_ERREUR (tentatives=10)`
On abandonne apres maxTentativesTotal pour ne pas spammer indefiniment.