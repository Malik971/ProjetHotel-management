# Rotation des credentials Pastell par derivation HMAC

Ce document explique comment l'authentification HTTP Basic entre sejour-backend et pastell-mock est geree en production, sans jamais stocker en clair un mot de passe partage dans une variable d'environnement.

## Probleme initial

L'API Pastell utilise HTTP Basic, un protocole qui transporte un couple `username:password` encode en base64 a chaque requete. Pour le mock en local, on definit les valeurs en clair dans `.env` :

```
PASTELL_MOCK_USERNAME=sejour-user
PASTELL_MOCK_PASSWORD=sejour-pass
```

Cette approche fonctionne mais a deux defauts en production :

1. Le mot de passe est statique. S'il fuit, il reste valide indefiniment.
2. Pour le deploiement sur Railway, il faut maintenir la meme valeur en clair sur deux services distincts (sejour et mock). Une desynchronisation casse l'authentification en silence.

## Approche retenue : derivation HMAC

Plutot que de stocker un username et un password, on stocke un secret maitre unique, partage entre les deux services. A partir de ce secret, chaque service calcule independamment les credentials a chaque requete.

```
                    ┌─────────────────────────────┐
                    │   PASTELL_MASTER_SECRET     │
                    │   (variable d'env partagee) │
                    └──────────────┬──────────────┘
                                   │
                ┌──────────────────┴──────────────────┐
                │                                     │
                ▼                                     ▼
   ┌────────────────────────┐         ┌─────────────────────────────┐
   │ sejour-backend         │         │ pastell-mock                │
   │                        │         │                             │
   │ PastellCredentials     │         │ MockCredentialsProvider     │
   │ Provider               │         │                             │
   │                        │         │                             │
   │ getUsername() = ...    │ ───►    │ verifie le username recu    │
   │ getCurrentPassword()   │  HTTP   │ verifie password = today    │
   │   = ...                │ Basic   │   OU password = yesterday   │
   └────────────────────────┘         └─────────────────────────────┘
```

Le username et le password sont derives de ce secret. Aucune communication n'est necessaire entre les deux services pour les synchroniser : la derivation est strictement deterministe.

## Algorithme

```
username = "sejour-" + hex(HMAC-SHA256(master_secret, "username"))[0..16]
password = base64url(HMAC-SHA256(master_secret, "password:" + UTC_DATE))[0..32]
```

* `HMAC-SHA256` est un standard cryptographique, disponible nativement dans toutes les JVM, deterministe pour un meme input.
* Le username utilise la chaine fixe `"username"` comme sel. Il ne change donc jamais (tant que le secret maitre ne change pas).
* Le password utilise la date UTC du jour comme sel, ce qui le fait tourner automatiquement chaque jour a minuit UTC.
* On garde uniquement les premiers caracteres : 16 hex pour le username (suffit pour l'unicite), 32 base64 pour le password (192 bits effectifs).

Exemple avec un secret `a3f29c8f1d4b7e6a8c5d9b2e4f1a7d6b` :

```
username = "sejour-a3f29c8f1d4b7e6a"        (stable)
password = "u8K7n3M9pL2qR5sT1vW6xY4z..."    (change chaque jour UTC)
```

## Pourquoi le username ne tourne pas

Spring Security identifie un `UserDetails` par son username, puis verifie le password. Si le username changeait chaque jour, le `UserDetailsService` du mock devrait gerer un mapping date - username, ce qui complique le code pour zero benefice securite reel.

Seul le password porte la fraicheur temporelle. C'est suffisant : un mot de passe leake ne reste valide que vingt-quatre heures (en pratique, voir la section tolerance).

## Tolerance au changement de jour

A minuit UTC, le password derive change. Une requete en vol au moment du basculement, ou un drift d'horloge entre deux conteneurs Railway, peut faire que sejour envoie encore le password d'hier alors que le mock attend deja celui d'aujourd'hui.

Sans tolerance, ces secondes produiraient des 401 qui declencheraient inutilement le retry du Lot 4.

Solution : le mock accepte simultanement le password d'aujourd'hui et celui d'hier. Implementation dans `RotatingPasswordEncoder` :

```java
public boolean matches(CharSequence rawPassword, String encodedPassword) {
    String raw = rawPassword.toString();
    return raw.equals(provider.getCurrentPassword())
        || raw.equals(provider.getYesterdayPassword());
}
```

Cout securite : un mot de passe leake reste valide vingt-quatre heures apres son expiration officielle, soit quarante-huit heures au total. Pour un portfolio, c'est acceptable. Pour une vraie prod, on passerait a une rotation horaire ce qui ramenerait la fenetre a deux heures.

## Pourquoi pas BasicAuthenticationInterceptor de Spring

Spring fournit `BasicAuthenticationInterceptor` qui pose le header `Authorization: Basic ...` sur le `RestClient`. Il prend username et password au constructeur et les fige a la creation du bean.

Si on l'utilisait avec un password rotatif, il enverrait indefiniment la valeur d'aujourd'hui meme apres minuit UTC. Il faudrait recreer le bean tous les jours.

A la place, on a `RotatingBasicAuthInterceptor` qui recalcule le couple username + password a chaque requete via le provider. Cout : une derivation HMAC-SHA256 par requete sortante, soit quelques microsecondes. Negligeable.

## Bascule dev <-> prod

Le code est ecrit pour fonctionner dans les deux modes :

* **Mode statique (dev local, CI, defaut) :** la propriete `pastell.master-secret` n'est pas definie. Le mock lit `pastell.mock.username` et `pastell.mock.password` en clair, sejour-backend lit `pastell.username` et `pastell.password`. Comportement identique au Lot 2.
* **Mode rotatif (prod, Lot 6) :** la propriete `pastell.master-secret` est definie sur les deux services. Les credentials statiques sont ignores, la derivation prend le relais.

La bascule est implementee via `@ConditionalOnProperty` et `ObjectProvider` cote consommateurs. Aucun changement de code n'est necessaire entre les deux modes, seules les variables d'env different.

## Generation du secret maitre

```bash
openssl rand -hex 32
```

Cette commande produit soixante-quatre caracteres hexadecimaux, soit 256 bits d'entropie. Largement suffisant pour resister a un brute force sur HMAC-SHA256.

A reporter sur les deux services Railway comme variable d'environnement `PASTELL_MASTER_SECRET`. Si tu modifies la valeur, il faut redeployer les deux services en meme temps, sinon le password d'hier accepte cote mock ne suffit pas a couvrir le decalage.

## Limites assumees

* Si le secret maitre fuit, l'attaquant peut deriver tous les passwords passes et futurs. La rotation quotidienne n'a aucune valeur si le secret lui-meme est compromis.
* Aucune revocation possible sans changer le secret et redeployer.
* Pas de protection contre un attaquant interne Railway qui aurait acces aux variables d'env de l'organisation.

Ces limites sont acceptables pour le contexte (portfolio public, demo). Pour une vraie integration Pastell, l'authentification ne passerait plus par ce mecanisme : Pastell fournit ses propres credentials avec des rotations gerees par leur cote.
