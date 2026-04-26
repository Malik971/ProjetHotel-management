# Backend — Spring Boot API

> `src/main/java/com/example/springhotel/`

---

## Principe général

Le backend suit une architecture **3 couches strictes** :

```
HTTP Request
     │
     ▼
[Controller]   ← reçoit, valide, délègue
     │
[Service]      ← logique métier, calculs, règles
     │
[Repository]   ← requêtes SQL via JPA
     │
  [MySQL]
```

Un Controller qui appelle directement un Repository est une **dette technique** à corriger (voir `HotelController`).

---

## `entity/` — Le modèle de données

Ce sont les classes mappées sur les tables MySQL via JPA/Hibernate.

### `Hotel.java`
Représente un hôtel.  
**Champs clés :** `nom`, `ville`, `latitude`, `longitude`, `prixMoyenNuit`, `categorie` (nb étoiles), `equipements` (liste).  
**Relation :** `@OneToMany` vers `Chambre` — un hôtel a plusieurs chambres.  
**Attention :** `@JsonIgnore` sur `chambres` pour éviter la sérialisation cyclique Hotel → Chambre → Hotel.

### `Chambre.java`
Représente une chambre dans un hôtel.  
**Champs clés :** `nom`, `prixParNuit` (BigDecimal), `capacity`, `superficie`, `typeLit`, `equipment`, `imageUrls`.  
**Relations :** `@ManyToOne` vers `Hotel`, `@OneToMany` vers `Reservation`.  
**Pattern :** `addReservation()` / `removeReservation()` maintiennent la cohérence bidirectionnelle manuellement.

### `Reservation.java`
Représente une réservation client.  
**Champs clés :** `dateDebut`, `dateFin`, `nomClient`, `emailClient`, `prixTotal`, `statut` (enum), `codeConfirmation`.  
**Enum `StatutReservation` :** `EN_ATTENTE` → `CONFIRMEE` → `TERMINEE` / `ANNULEE`.  
**Relations :** `@ManyToOne` vers `Chambre` et vers `Users`.

### `Users.java` + `Role.java` + `Privilege.java`
Système de sécurité à 3 niveaux : un User a des Roles, un Role a des Privileges.  
**Fetch :** `EAGER` sur les rôles (chargés immédiatement avec l'utilisateur, nécessaire pour Spring Security).

---

## `controller/` — Les points d'entrée HTTP

Chaque Controller expose un groupe de routes REST.

### `HotelController.java`
**Routes :** `GET /api/hotels`, `GET /api/hotels/{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`  
**Particularité :** supporte deux formats : `application/json` (sans image) et `multipart/form-data` (avec upload image).  
**⚠️ Dette :** appelle `hotelRepository` directement — à migrer vers `HotelService`.  
**Bug corrigé :** la méthode `PUT` mappait les champs de base mais oubliait `latitude`, `longitude`, `prixMoyenNuit`, `categorie`, `equipements`.

### `HotelSearchController.java`
**Routes :** `POST /api/hotels/search`, `GET /api/hotels/all`  
Délègue à `HotelSearchService` avec un `HotelSearchDTO` contenant les critères de recherche.

### `ChambreController.java`
**Routes :** CRUD complet + `GET /api/chambres/disponibles?dateDebut=&dateFin=&hotelId=`  
La route de disponibilité construit un `DisponibiliteDTO` à partir des query params et délègue à `ChambreService`.

### `ReservationController.java`
**Route :** `POST /api/reservations`  
Reçoit un `ReservationRequestDTO`, récupère l'email de l'utilisateur connecté via `Authentication`, délègue à `ReservationService`.

### `ClientReservationController.java`
**Routes :** `GET /api/client/reservations/mes-reservations`, `GET /{id}`, `DELETE /{id}`  
Accessible uniquement aux utilisateurs connectés. Vérifie que la réservation appartient bien à l'utilisateur avant toute action.

### `LoginController.java`
**Route :** `POST /api/v1/login`  
Vérifie email + mot de passe (BCrypt), retourne un objet `UserResponse` avec id, email, firstName, lastName, roles.  
**⚠️ À faire :** remplacer la réponse objet par un **token JWT** pour passer en authentification stateless.

### `RegistrationLoginController.java`
**Route :** `POST /api/v1/register?role=USER`  
Valide email, longueur password, unicité. Encode le password (BCrypt). Assigne `ROLE_USER` par défaut.

### `AdminUserController.java`
**Routes :** `GET /api/admin/users`, `POST /api/admin/users`, `DELETE /api/admin/users/{id}`  
**⚠️ Sécurité :** ces routes doivent être protégées avec `@PreAuthorize("hasRole('ROLE_ADMIN')")`.

---

## `dto/` — Les objets de transfert

Les DTOs séparent la représentation API du modèle interne.

| DTO | Usage |
|-----|-------|
| `HotelSearchDTO` | Critères de recherche hotel (ville, prix, dates, équipements…) |
| `ChambreDTO` | Création / modification d'une chambre |
| `DisponibiliteDTO` | Vérification de disponibilité (chambreId + dates) |
| `ReservationRequestDTO` | Données du formulaire de réservation |
| `ReservationResponseDTO` | Ce que l'API renvoie après création/lecture d'une réservation |

---

## Migrations Flyway

Les scripts SQL versionnés sont dans `src/main/resources/db/migration/`.  
Format : `V{version}__{description}.sql` — ex: `V1__create_hotel_table.sql`.  
Flyway les exécute **dans l'ordre** au démarrage. Ne jamais modifier un script déjà appliqué en prod.

---

## Ce qui reste à faire

- Migrer `HotelController` vers `HotelService`
- Implémenter JWT dans Spring Security
- Ajouter `@Valid` sur tous les `@RequestBody`
- Créer un `GlobalExceptionHandler` avec `@ControllerAdvice`
- Aligner `prixTotal` en `BigDecimal` dans `Reservation` (actuellement `Double`)
- Ajouter `@PreAuthorize` sur les routes admin
- Écrire les tests unitaires (starters déjà présents dans `pom.xml`)