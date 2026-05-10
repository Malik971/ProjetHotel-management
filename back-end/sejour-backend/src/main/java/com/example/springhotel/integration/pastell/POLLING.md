# Lot 5 — Livraison

Synchronisation descendante Pastell -> Sejour, livrée d'un bloc.

---

## Récap des décisions architecturales validées

1. **Curseur de polling** : `idJ` monotone (pas de timestamp).
2. **Stockage du curseur** : table dédiée mono-ligne `pastell_polling_cursor`.
3. **Idempotence** : pas de table `pastell_journal_processed`, le curseur seul suffit.
4. **idD inconnu côté Sejour** : log WARN avec idD et idJ, on avance.
5. **Bascule `terminee`** : option A retenue, Pastell pilote la fin de vie du dossier
   (réversible en option B plus tard sans rien casser).
6. **Toggle `pastell.polling.enabled`** : ajouté.

---

## Déviation versus le plan annoncé

J'avais annoncé **7 fichiers de prod et 5 de test**. La livraison est à **9 fichiers
de prod et 6 de test**. Voici pourquoi :

### +1 fichier de prod : `PastellJournalEntryProcessor.java`

En relisant mon code, j'ai réalisé que `runPollOnce()` appelait `processEntry()`
dans le **même bean**. Or Spring AOP n'intercepte pas les auto-calls (`this.method()`),
donc l'annotation `@Transactional REQUIRES_NEW` aurait été silencieusement ignorée.
Risque réel en production : un `save()` sur `Reservation` réussit, le `save()`
sur `PastellSync` échoue, base incohérente.

J'ai externalisé `processEntry()` dans un bean dédié `PastellJournalEntryProcessor`,
exactement comme le Lot 4 a externalisé `retraiterSync()` dans `PastellSyncService`
(appelé depuis `PastellRetryScheduler`). Architecture cohérente avec le reste du
projet et transactions effectives.

### +1 fichier de test : `PastellJournalEntryProcessorTest.java`

Conséquence directe : il faut un test dédié au processor (où vit toute la logique
métier de traitement d'une entrée). Le test du service `PastellInboundSyncServiceTest`
se concentre maintenant sur l'orchestration (lecture curseur, boucle, avancement).

### +1 fichier de test : `PastellClientJournalTest.java` (au lieu d'étendre `PastellClientTest`)

Je l'avais signalé en début de livraison : ajouter 6 tests à un fichier déjà à
250 lignes aurait été pénible à relire dans la PR. Fichier séparé, cohérent avec
la séparation existante entre `PastellClientTest` et `PastellClientWithRetryTest`.

---

## Arborescence cible dans ton repo

À placer **dans le module `sejour-backend`** (pas `pastell-mock` qui n'a aucune
modification dans ce lot).

### Migration Flyway

```
sejour-backend/src/main/resources/db/migration/
└── V4__pastell_polling_cursor_table.sql      [NOUVEAU]
```

### Code de production

```
sejour-backend/src/main/java/com/example/springhotel/integration/pastell/
├── client/
│   ├── PastellClient.java                    [REMPLACER : ajout fetchJournalSince]
│   └── PastellJournalEntry.java              [NOUVEAU]
├── config/
│   └── PastellProperties.java                [REMPLACER : ajout polling.enabled]
├── entity/
│   └── PastellPollingCursor.java             [NOUVEAU]
├── policy/
│   └── PastellActionMapper.java              [NOUVEAU]
├── repository/
│   └── PastellPollingCursorRepository.java   [NOUVEAU]
├── scheduler/
│   └── PastellPollingScheduler.java          [NOUVEAU]
└── service/
    ├── PastellInboundSyncService.java        [NOUVEAU]
    └── PastellJournalEntryProcessor.java     [NOUVEAU]
```

### Tests

```
sejour-backend/src/test/java/com/example/springhotel/integration/pastell/
├── client/
│   └── PastellClientJournalTest.java         [NOUVEAU]
├── policy/
│   └── PastellActionMapperTest.java          [NOUVEAU]
├── scheduler/
│   └── PastellPollingSchedulerTest.java      [NOUVEAU]
├── service/
│   ├── PastellInboundSyncServiceTest.java    [NOUVEAU]
│   └── PastellJournalEntryProcessorTest.java [NOUVEAU]
└── e2e/
    └── PastellPollingEndToEndTest.java       [NOUVEAU]
```

### Documentation

```
sejour-backend/src/main/java/com/example/springhotel/integration/pastell/
└── POLLING.md                                [NOUVEAU]
```

(À côté de `RETRY.md` qui est au même endroit, par cohérence.)

---

## Liste exhaustive des fichiers livrés

| Fichier                                  | Type              | Statut       |
|------------------------------------------|-------------------|--------------|
| `V4__pastell_polling_cursor_table.sql`   | Migration         | Nouveau      |
| `PastellPollingCursor.java`              | Entité JPA        | Nouveau      |
| `PastellPollingCursorRepository.java`    | Repository        | Nouveau      |
| `PastellJournalEntry.java`               | DTO record        | Nouveau      |
| `PastellClient.java`                     | Client HTTP       | À remplacer  |
| `PastellActionMapper.java`               | Policy            | Nouveau      |
| `PastellJournalEntryProcessor.java`      | Service           | Nouveau      |
| `PastellInboundSyncService.java`         | Service           | Nouveau      |
| `PastellPollingScheduler.java`           | Scheduler         | Nouveau      |
| `PastellProperties.java`                 | Configuration     | À remplacer  |
| `PastellActionMapperTest.java`           | Test unitaire     | Nouveau      |
| `PastellClientJournalTest.java`          | Test WireMock     | Nouveau      |
| `PastellJournalEntryProcessorTest.java`  | Test Mockito      | Nouveau      |
| `PastellInboundSyncServiceTest.java`     | Test Mockito      | Nouveau      |
| `PastellPollingSchedulerTest.java`       | Test Mockito      | Nouveau      |
| `PastellPollingEndToEndTest.java`        | Test E2E          | Nouveau      |
| `POLLING.md`                             | Doc DevRel        | Nouveau      |

**17 fichiers au total** : 1 migration, 9 prod, 6 tests, 1 doc.

---

## Configuration

Aucune nouvelle propriété **obligatoire** à ajouter dans `application.properties`.
Les défauts sont :

- `pastell.polling.enabled=true` (par défaut Java dans la classe `Polling`)
- `pastell.polling.interval-ms=30000` (par défaut Java dans la classe `Polling`)

⚠️ **Mais attention** : comme le pattern du Lot 4 (`pastell.retry.scheduler-enabled`),
Spring `@ConditionalOnProperty` **sans `matchIfMissing=true`** considère qu'une
propriété absente vaut "false". Si tu veux que le polling se déclenche en prod,
tu dois explicitement ajouter dans ton fichier de config :

```properties
pastell.polling.enabled=true
pastell.polling.interval-ms=30000
```

C'est **identique** au comportement actuel de `pastell.retry.scheduler-enabled`.
Si tu veux changer ce pattern global pour mettre `matchIfMissing=true` partout,
ce serait une modif à part qui touche aussi le Lot 4. Pour l'instant, je reproduis
le pattern existant.

---

## Comment vérifier la livraison

### 1. Compiler

```bash
mvn -pl sejour-backend clean compile
```

Doit passer sans erreur.

### 2. Lancer les tests

```bash
mvn -pl sejour-backend test
```

Attendu :
- 70 tests verts existants (Lots 1+2+3+4) **inchangés**
- ~25 nouveaux tests verts pour le Lot 5 :
    - `PastellActionMapperTest` : ~14 tests
    - `PastellClientJournalTest` : 6 tests
    - `PastellJournalEntryProcessorTest` : 10 tests
    - `PastellInboundSyncServiceTest` : 5 tests
    - `PastellPollingSchedulerTest` : 3 tests
    - `PastellPollingEndToEndTest` : 2 tests

**Total visé : ~95 tests verts.**

### 3. Démo manuelle

Voir la section "Démo manuelle de bout en bout" dans `POLLING.md`.

---

## Ce que je n'ai PAS touché

Pour confirmer que cette livraison est purement additive :

- ❌ Pas de modification de `ReservationService` (la sync montante reste identique)
- ❌ Pas de modification de `PastellSyncService` (le Lot 3/4 reste identique)
- ❌ Pas de modification de `PastellClientWithRetry` (le wrapper Lot 4 reste identique)
- ❌ Pas de modification de `PastellRetryScheduler` (le scheduler de retry reste identique)
- ❌ Pas de modification de `pastell-mock` (l'endpoint `GET /api/v2/journal` était
  déjà prêt depuis le Lot 2)
- ❌ Pas de modification de `application-prod.properties` ni `application.properties`
  (Pastell pas encore activé en prod, c'est cohérent avec ton plan de merge)
- ❌ Pas de modification de `application-test.properties` (les tests E2E ajoutent
  leurs propriétés via `@TestPropertySource`)

Les **deux seuls fichiers existants à remplacer** sont `PastellClient.java` (ajout
de `fetchJournalSince`) et `PastellProperties.java` (ajout du toggle `polling.enabled`).
Tout le reste est nouveau.

---

## Prochaine étape recommandée

Une fois cette livraison validée :

1. Tu copies les fichiers aux bons endroits (cf. arborescence ci-dessus).
2. Tu lances `mvn -pl sejour-backend clean test` pour vérifier que les 133 tests passent.
3. Tu fais la démo manuelle décrite dans `POLLING.md` (Postman + observation des
   logs et de la base).
4. Tu commit avec un message du genre :

   ```
   feat(pastell): lot 5 - sync descendante par polling du journal
   feat(pastell): lot 5 - downstream sync via journal polling

   - Add PastellPollingCursor entity + V4 migration
   - Add fetchJournalSince to PastellClient
   - Add PastellActionMapper, PastellJournalEntryProcessor,
     PastellInboundSyncService, PastellPollingScheduler
   - Add pastell.polling.enabled toggle
   - 25 new tests (unit + WireMock + E2E)
   - DevRel doc: POLLING.md

   Closes #lot-5
   ```

Le Lot 6 (observabilité + doc DevRel finale + finalisation portfolio) sera la
prochaine conversation.