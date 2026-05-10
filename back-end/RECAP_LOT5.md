# Lot 5 — Récap technique

> Synchronisation descendante Pastell → Sejour
> Document destiné à Steven (responsable dev Libriciel) pour relecture
> Et à moi-même (Malik) pour réviser

---

## 1. Ce que résout le Lot 5 en une phrase

Quand un agent change l'état d'un dossier directement dans Pastell (sans passer par Sejour), Sejour le détecte automatiquement et met à jour le statut de la réservation correspondante.

Avant le Lot 5 : flux à sens unique (Sejour → Pastell uniquement). Après : flux bidirectionnel.

---

## 2. Décision architecturale principale : polling, pas webhook

**Pourquoi polling** : Pastell n'expose pas de webhooks natifs aujourd'hui. On va donc chercher l'information nous-mêmes toutes les 30 secondes via `GET /api/v2/journal`.

> Mots-clés à chercher en Ctrl+F dans la discussion : "Pourquoi du polling et pas un webhook"

**Compromis assumé** :
- Latence : jusqu'à 30 secondes entre l'action dans Pastell et la mise à jour dans Sejour
- Charge : un appel HTTP toutes les 30 secondes même quand rien ne change
- Bénéfice : aucune dépendance à une API que Pastell n'a pas

---

## 3. Les 9 fichiers de production (et leur rôle)

| Fichier | Rôle | Mot-clé à chercher |
|---|---|---|
| `V4__pastell_polling_cursor_table.sql` | Migration Flyway créant la table mono-ligne du curseur | "table mono-ligne" |
| `PastellPollingCursor.java` | Entité JPA du curseur (PK forcée à 1) | "PrePersist" |
| `PastellPollingCursorRepository.java` | Repository Spring Data, expose `findCursor()` | "SINGLETON_ID" |
| `PastellJournalEntry.java` | DTO record qui mappe une entrée du journal Pastell | "JsonIgnoreProperties" |
| `PastellClient.java` | Ajout de `fetchJournalSince(long)` au client HTTP existant | "ParameterizedTypeReference" |
| `PastellActionMapper.java` | Table de décision : action Pastell → StatutReservation cible | "table de décision" |
| `PastellJournalEntryProcessor.java` | Bean qui traite UNE entrée dans une transaction propre | "auto-call" |
| `PastellInboundSyncService.java` | Orchestrateur : lit le curseur, boucle sur les entrées | "REQUIRES_NEW" |
| `PastellPollingScheduler.java` | Scheduler `@Scheduled(fixedDelay = 30s)` | "fixedDelay" |
| `PastellProperties.java` | Ajout du toggle `pastell.polling.enabled` | "Polling" |

---

## 4. Concepts clés (à comprendre avant de relire le code)

### Le curseur de polling

Une seule valeur globale persistée en base : `last_processed_id_j`. C'est le numéro de la dernière entrée du journal Pastell qu'on a déjà traitée. À chaque tick, on demande à Pastell tout ce qui est plus récent que cette valeur.

> Mot-clé : "Pourquoi pas un timestamp"

### L'idempotence par le curseur

Pas besoin de table `pastell_journal_processed` pour mémoriser ce qu'on a vu. Le curseur seul suffit, parce que :
- En marche normale, on demande toujours `since_id_j > last_processed`
- Si on rejoue par accident une entrée déjà traitée, l'action est idempotente (passer une réservation déjà ANNULEE en ANNULEE = no-op)

> Mot-clé : "L'idempotence : pourquoi le curseur seul suffit"

### Le mapping action → statut

Toutes les actions Pastell ne déclenchent pas un changement de statut Sejour. Seules `terminee` et `annulee` le font. Les autres (`creation`, `validee`, `confirmee`, etc.) sont neutres pour le statut Sejour, mais on rafraîchit quand même `pastell_etat_dernier_connu` pour la traçabilité.

> Mot-clé : "Le mapping action Pastell vers StatutReservation"

### La divergence

Si Pastell envoie une action qui contredit le statut Sejour (par exemple `annulee` sur une réservation déjà `TERMINEE`), on ne touche pas au statut Sejour (Spring reste autorité), mais on bascule le `SyncStatus` du `PastellSync` en `DIVERGENCE` pour signaler à un humain qu'il y a un arbitrage à faire.

> Mot-clé : "La divergence : quand Pastell et Sejour ne sont pas d'accord"

### L'auto-call problem (piège Spring AOP)

Initialement j'avais mis `processEntry()` dans le même bean que `runPollOnce()`. Spring AOP n'intercepte pas les auto-calls (`this.method()`), donc l'annotation `@Transactional REQUIRES_NEW` aurait été silencieusement ignorée. J'ai externalisé `processEntry` dans un bean séparé pour résoudre ce problème.

> Mot-clé : "auto-call" ou "PastellJournalEntryProcessor"

---

## 5. Stratégie transactionnelle

Trois opérations distinctes, trois transactions distinctes :

1. **L'appel HTTP** vers Pastell : pas dans une transaction (sinon connexion JDBC bloquée pendant l'attente réseau)
2. **Le traitement de chaque entrée** : `@Transactional REQUIRES_NEW` (isolation totale entre entrées)
3. **La mise à jour du curseur** : `@Transactional REQUIRES_NEW` (commit indépendant)

Conséquence : si une entrée échoue, les suivantes sont quand même traitées, et le curseur avance pour ne pas rebloquer indéfiniment.

> Mot-clé : "Stratégie transactionnelle"

---

## 6. Ce qui marche aujourd'hui

✅ Création réservation côté Sejour → dossier créé dans Pastell-mock (Lot 3, déjà OK avant le Lot 5)
✅ Action `validation` / `confirmation` / `terminaison` / `annulation` côté Pastell → entrée dans le journal
✅ Polling Sejour toutes les 30 secondes → consommation des nouvelles entrées
✅ Bascule automatique du statut Reservation quand action `terminee` ou `annulee` reçue
✅ Détection de DIVERGENCE quand action incohérente avec le statut courant
✅ Curseur persisté en base, survit aux redémarrages
✅ 133 tests verts (132 verts d'origine + 1 fix sur strict stubbing Mockito)

---

## 7. Ce qui ne marche pas (encore)

❌ **Pas d'observabilité** : pas de dashboard temps réel sur le polling, juste les logs Spring (prévu Lot 6)
❌ **Pas de notification admin sur DIVERGENCE** : aujourd'hui c'est un WARN dans les logs, il faut qu'un humain regarde
❌ **Pas de support multi-instances** : si un jour Sejour tourne sur 2 replicas Render, le curseur peut être lu/écrit en parallèle (besoin d'un `SELECT ... FOR UPDATE`)
❌ **Pas de webhook Pastell** : volontaire, Pastell ne l'expose pas, on a juste mis un placeholder dans les properties
❌ **Pas d'endpoint `/api/admin/pastell/poll`** : ah si, ajouté en cours de route pour la démo (`AdminPastellController`), mais c'est public sans auth pour l'instant

> Mot-clé : "Ce qui n'est pas dans le Lot 5"

---

## 8. Pourquoi tel choix vs tel autre

| Décision | Alternative envisagée | Raison du choix |
|---|---|---|
| Curseur = `id_j` monotone | Timestamp | `id_j` est sans collision possible, pas de souci de fuseau horaire |
| Table dédiée `pastell_polling_cursor` | Colonne dans `pastell_sync` | Mélangerait granularité globale (curseur) et granularité par réservation |
| PK forcée à 1 (CHECK SQL) | `@GeneratedValue` | Garantit qu'il ne peut jamais y avoir qu'une seule ligne |
| `terminee` Pastell → `TERMINEE` Sejour (option A) | Job interne Sejour qui termine les réservations passées (option B) | Réversible facilement (retirer 1 case dans le mapper). Option A permet la démo aujourd'hui, option B viendra peut-être plus tard |
| Pas de retry sur le polling | Niveau 2 de retry comme le Lot 4 | La fréquence (30s) sert d'effet retry naturel |
| Avancer le curseur même sur entrée en échec | Bloquer sur la première entrée problématique | Sinon une entrée corrompue bloquerait tout le polling indéfiniment |
| `PastellJournalEntryProcessor` séparé | `processEntry` dans le service | Spring AOP ignore les auto-calls (transaction REQUIRES_NEW serait perdue) |

---

## 9. Liens vers les autres Lots (contexte historique)

- **Lot 1** : structure multi-module Maven (sejour-backend + pastell-mock séparés)
- **Lot 2** : mock Pastell qui implémente l'API `/api/v2/document` et `/api/v2/journal`
- **Lot 3** : sync montante (création réservation Sejour → création dossier Pastell)
- **Lot 4** : retry sophistiqué sur la sync montante (2 niveaux : court immédiat + reprise différée scheduler)
- **Lot 5** : sync descendante (CE LOT)
- **Lot 6** (à venir) : observabilité, métriques Micrometer, dashboard

---

## 10. Pour Steven, en pratique

Si tu veux comprendre rapidement le Lot 5, je suggère cet ordre de lecture :

1. **POLLING.md** (la doc DevRel-style) → vue d'ensemble narrative
2. **PastellActionMapper.java** → la logique métier en 100 lignes
3. **PastellJournalEntryProcessor.java** → le cœur du traitement
4. **PastellInboundSyncService.java** → l'orchestration
5. **PastellPollingEndToEndTest.java** → le scénario de bout en bout, le plus parlant

Si tu veux la démo en local :
- Branche `feat/pastell-lot1-multi-module`
- README à la racine du module pour démarrer
- Dashboard HTML dans `_WORKSPACE/dashboard/springhotel-pastell-demo.html` (CSS/JS purs, ouverts via `python -m http.server`)
- Collection Postman dans le même dossier

---

## 11. Mes interrogations honnêtes (à discuter)

Trois points sur lesquels j'ai pris une décision mais où une autre voie était défendable :

1. **Faut-il un endpoint d'admin pour purger ou réinitialiser le curseur ?** Aujourd'hui non, pour réinitialiser il faut faire un UPDATE SQL direct. C'est peut-être à ajouter.

2. **Faut-il logger en INFO ou WARN quand on saute une entrée pour `idD` inconnu ?** J'ai mis WARN parce que c'est un cas qui mérite l'attention d'un admin, mais ça pourrait noyer les logs si beaucoup de dossiers sont créés directement dans Studio sans passer par Sejour.

3. **Faut-il rendre `pastell.polling.enabled` à `true` par défaut quand `pastell.enabled=true` ?** Aujourd'hui c'est `false` par défaut (il faut explicitement l'activer). Cohérent avec le pattern du Lot 4 (`retry.scheduler-enabled`), mais peut-être contre-intuitif pour quelqu'un qui découvre.

> Mot-clé pour retrouver la discussion sur ces points : "matchIfMissing"