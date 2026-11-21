# Rapport de réalisation — Lisa Cosmétique et Bien-être

> **Objet du document.** Ce rapport explique **tout ce qui a été fait** pour construire la
> plateforme : le cadrage, les choix techniques, les phases de développement, le modèle de
> données, l'API, l'interface, la sécurité, les tests, les problèmes rencontrés et les
> limites restantes. Il sert à la fois de mémoire du projet et de guide de reprise.
>
> Il a été reconstitué à partir du dépôt lui-même (code, migrations Flyway, tests,
> configuration, documentation existante) et vérifié par une exécution de la suite de tests
> backend le 16 novembre 2026.

---

## 1. Le besoin de départ

Construire une **boutique en ligne pour Lisa Cosmétique et Bien-être**, au Cameroun, avec :

- un catalogue de produits (cosmétiques, soins corporels, perruques, mèches, accessoires,
  entretien capillaire) ;
- un panier utilisable **sans créer de compte** ;
- la création d'une commande avec livraison par zone ;
- un **suivi de commande public** sécurisé ;
- un **espace d'administration** pour le catalogue et les commandes.

Contraintes structurantes retenues dès le départ :

| Contrainte | Conséquence |
|---|---|
| Pas de Docker en local | Base et fichiers hébergés par **Supabase** |
| Paiement mobile money non disponible immédiatement | Commande créée en `PENDING`, **aucun paiement en ligne** ; le règlement est convenu avec la boutique |
| Montants en francs CFA | Entiers `bigint` (XAF), jamais de décimaux |
| Mobile first | Interface responsive, panier invité |
| Secrets hors Git | Seuls les `.env.example` sont versionnés |

---

## 2. Stack et architecture retenues

### 2.1 Technologies

| Couche | Choix |
|---|---|
| Frontend | **Next.js 16** (App Router), React 19, TypeScript, Tailwind CSS 4 |
| Données distantes | **TanStack Query 5** |
| État local | TanStack Query ; panier identifié par un jeton en `localStorage`. Zustand est installé mais pas encore utilisé |
| Formulaires | Formulaires React natifs ; React Hook Form + Zod sont installés mais pas encore employés |
| Backend | **Spring Boot 3.5.16**, Java 21, Maven (wrapper `mvnw`) |
| Persistance | PostgreSQL Supabase via JPA/Hibernate, **Flyway** pour le schéma |
| Fichiers | **Supabase Storage**, bucket privé, URLs signées |
| Sécurité | Spring Security, HTTP Basic pour l'admin, BCrypt |
| Documentation API | springdoc OpenAPI / Swagger UI |
| Tests | JUnit 5, Spring Boot Test, MockMvc, **H2 en mode PostgreSQL** |

Aucun conteneur, aucun service local à installer hormis Node.js et un JDK 21.

### 2.2 Vue d'ensemble

```
Navigateur ──HTTPS──▶ frontend/ (Next.js) ──REST /api/v1──▶ backend/ (Spring Boot)
                                                              │
                                                              ├──JDBC/SSL──▶ Supabase PostgreSQL
                                                              └──REST+clé──▶ Supabase Storage
                                                                             (bucket privé)
```

Règle d'or : **le frontend ne détient aucun secret**. Il ne connaît que
`NEXT_PUBLIC_API_URL`. Il ne parle jamais directement à Supabase. Le backend est la seule
source de vérité pour les prix, stocks, frais, remises et statuts.

### 2.3 Organisation du dépôt

```
lisa-cosmetique/
├── frontend/          Application Next.js
├── backend/           API Spring Boot
├── infrastructure/    Procédure Supabase
├── docs/              Architecture, base de données, administration, revue, ce rapport
└── README.md          Installation, démarrage, URLs, sécurité
```

---

## 3. Déroulé du travail, phase par phase

### Phase 0 — Cadrage et socle

1. Choix de la stack et de l'hébergement Supabase (PostgreSQL + Storage), sans Docker.
2. Création de l'arborescence `frontend/`, `backend/`, `infrastructure/`, `docs/`.
3. Mise en place des fichiers d'environnement **exemple** (`.env.example`) et des
   `.gitignore` pour qu'aucun secret ne soit versionné.
4. Configuration Maven avec le wrapper, Java 21, Spring Boot 3.5.16.

### Phase 1 — Démarrage sans base de données

Objectif : pouvoir valider la communication frontend ↔ backend **avant** même d'avoir un
projet Supabase.

- Ajout du profil Spring **`nodb`** qui exclut DataSource, JPA, Hibernate et Flyway.
- Endpoint `GET /api/v1/health` (public) et `GET /actuator/health`.
- En-tête du site affichant l'état de connexion à l'API.
- CORS explicite, sans `*`, avec exposition de l'en-tête `X-Cart-Token`.
- Gestion d'erreurs centralisée au format **RFC 9457** (`application/problem+json`).

### Phase 2 — Base de données et modèle métier

- Écriture des migrations Flyway **V1 → V10** : 15 tables, contraintes, index, RLS.
- Création des entités JPA des domaines `user`, `address`, `catalog`, `promotion`,
  `cart`, `order`, `payment`, `delivery`, plus l'audit commun.
- Création des repositories et des services métier :
  `InventoryService`, `OrderService`, `CartService`, `PromotionService`,
  `DeliveryZoneService`, `PaymentService`, et le contrat `PaymentProvider`.
- DTO, mappers, exceptions métier.
- Jeu de démonstration **séparé** (`db/demo`), chargé uniquement en profil `dev`.
- Première revue de code formalisée dans `docs/CODE_REVIEW_PHASE2.md`.

### Phase 3 — API publique

- Catalogue : catégories, liste paginée/filtrable/triable, fiche produit par slug.
- Panier : consultation, ajout, modification de quantité, suppression, vidage.
- Commande : création transactionnelle avec recalcul complet des montants.
- Suivi public : numéro de commande + téléphone.
- Zones de livraison publiques.

### Phase 4 — Administration et interface

- API d'administration du catalogue (CRUD produits/catégories, suppression logique).
- API d'administration des commandes (recherche, détail, transitions de statut,
  expédition, livraison, annulation avec restitution du stock).
- Amorçage automatique du premier compte `ADMIN`.
- Frontend complet : accueil, boutique, fiche produit, panier, commande, suivi,
  back-office commandes.
- Stockage Supabase : bucket privé créé au démarrage, upload/URL signée/suppression.

### Phase 5 — Durcissement, tests et documentation

- Tests d'intégration H2 sur le catalogue, le panier, les commandes, l'administration,
  le paiement et le stockage.
- Corrections issues de la revue (voir section 9).
- Documentation : `README.md`, `docs/ARCHITECTURE.md`, `docs/DATABASE.md`,
  `docs/ADMIN_CATALOG.md`, `infrastructure/README.md`.

---

## 4. Backend en détail

### 4.1 Découpage par domaine

Chaque domaine contient ses propres `controller`, `service`, `repository`, `entity`,
`dto` et éventuellement `mapper`. Les entités JPA **ne sortent jamais** d'un service :
l'API n'expose que des DTO.

| Domaine | Contenu |
|---|---|
| `config` | Sécurité, CORS, OpenAPI, propriétés d'amorçage admin |
| `exception` | Exceptions métier + handler global RFC 9457 |
| `health` | Endpoint de disponibilité |
| `storage` | Interface `StorageService` + implémentation Supabase |
| `common.audit` | `created_at`, `updated_at`, auteurs JPA |
| `catalog` | Catégories, produits, images, lecture publique et administration |
| `user` | Comptes, rôles, chargement des utilisateurs, amorçage admin |
| `address` | Adresses de livraison (entité/repository) |
| `promotion` | Remises et calcul de la meilleure offre |
| `cart` | Panier client et invité |
| `order` | Commandes, lignes, stock, suivi, back-office |
| `payment` | Paiements, événements idempotents, contrat prestataire |
| `delivery` | Zones de livraison et expéditions |

### 4.2 Profils Spring

| Profil | Usage | Effet |
|---|---|---|
| `nodb` | Démarrage avant configuration Supabase | Aucune base, aucun domaine métier exposé ; seuls santé et connectivité |
| `dev` | Développement | Flyway + jeu de démonstration |
| `prod` | Production | Swagger désactivé par défaut |
| `test` | Tests | H2, valeurs factices, aucun service externe |

Les services, contrôleurs et l'initialiseur admin sont annotés `@Profile("!nodb")` ;
en profil `nodb`, ils ne sont simplement pas chargés.

### 4.3 Sécurité

- API **stateless** : pas de session, CSRF désactivé, formulaire de connexion désactivé.
- **HTTP Basic** pour l'administration, adossé à la table `users` (mots de passe BCrypt).
- Routes publiques :
  - `/api/v1/health`, `/actuator/health`, Swagger ;
  - `GET /api/v1/categories/**`, `GET /api/v1/products/**`,
    `GET /api/v1/delivery-zones` ;
  - `/api/v1/cart/**` ;
  - `POST /api/v1/orders` et `POST /api/v1/orders/track`.
- `/api/v1/admin/**` exige le rôle `ADMIN`.
- Toute autre route exige une authentification.
- CORS : origines explicites via `CORS_ALLOWED_ORIGINS`, méthodes limitées, en-tête
  `X-Cart-Token` exposé au navigateur.
- Les erreurs ne révèlent ni trace technique ni message interne.

**Amorçage du premier administrateur :** au démarrage, si `ADMIN_EMAIL` et
`ADMIN_PASSWORD` sont fournis (8 caractères minimum) et que le compte n'existe pas, il est
créé avec le rôle `ADMIN`. Un compte existant n'est jamais modifié.

### 4.4 Gestion des erreurs

Un `@RestControllerAdvice` traduit les exceptions en `ProblemDetail` :

| Exception | Statut |
|---|---|
| `ResourceNotFoundException` | `404` |
| `ConflictException` | `409` |
| `BusinessException` | `422` |
| `AccessDeniedException` | `403` |
| Validation Bean Validation | `400` avec le détail par champ |
| Exception inattendue | `500`, détail technique uniquement dans les logs |

### 4.5 Panier

- Un client authentifié utilise son panier utilisateur.
- Un visiteur est identifié par un **jeton opaque** `X-Cart-Token` :
  - généré par l'API au premier appel s'il est absent ;
  - renvoyé dans l'en-tête de réponse ;
  - conservé par le frontend en `localStorage` ;
  - stocké en base **uniquement sous forme de hash SHA-256**.
- Ajout, mise à jour (quantité 0 = suppression), retrait, vidage.
- Le contrôle de stock à l'ajout est **indicatif** : la vérité reste la création de
  commande sous verrou.

### 4.6 Création de commande — le cœur métier

Dans une seule transaction :

1. Regroupement et validation des lignes (produit unique, quantité > 0).
2. **Verrouillage pessimiste** (`SELECT … FOR UPDATE`) des produits, triés par identifiant
   pour éviter les interblocages.
3. Vérification que chaque produit existe, est actif et dispose du stock demandé.
4. Recalcul du sous-total à partir des **prix en base** (aucun prix client accepté).
5. Lecture de la zone de livraison active et de ses frais.
6. Calcul de la **meilleure promotion** applicable.
7. Enregistrement de la commande et de ses lignes (nom, SKU et prix figés).
8. Décrémentation du stock.

Toute erreur annule l'ensemble. Le numéro de commande suit le format
`LC-AAAAMMJJ-XXXXXX` (6 caractères aléatoires, unicité en base, 5 tentatives).

**Annulation :** possible uniquement depuis `PENDING` ou `CONFIRMED`, idempotente, refuse
une commande déjà payée, et restitue le stock une seule fois sous verrou.

### 4.7 Suivi public

Le client fournit le **numéro de commande et le téléphone**. Le téléphone est normalisé
(indicatif `+237`/`00237`, espaces, séparateurs) avant comparaison. La réponse n'expose ni
adresse, ni e-mail, ni téléphone. Toute discordance renvoie uniformément `404`, pour ne pas
révéler l'existence d'une commande.

### 4.8 Back-office des commandes

- Recherche paginée par statut et par texte, tri par date décroissante.
- Détail avec coordonnées, adresse, lignes et expédition.
- Transitions contrôlées :
  `PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED`,
  plus `CANCELLED` depuis `PENDING` ou `CONFIRMED`.
- L'expédition crée/met à jour un `Shipment`, avec numéro de suivi facultatif et unique.
- La livraison renseigne les dates d'expédition et de livraison.
- Une commande livrée ou annulée est close.

### 4.9 Stockage Supabase

- Activé uniquement si `SUPABASE_SECRET_KEY` est renseignée.
- Le bucket privé est créé au démarrage s'il n'existe pas ; si Supabase est injoignable,
  **l'API démarre quand même** (seules les fonctions fichiers échouent).
- Contrat `StorageService` : `upload`, `presignedDownloadUrl`, `delete`.
- Les clés d'objet sont générées côté serveur, les URLs signées ont une durée limitée
  (`SUPABASE_SIGNED_URL_TTL`, 15 min par défaut).
- Les URLs signées renvoyées par Supabase sont relatives à `/storage/v1` : le service les
  préfixe correctement.
- Particularité traitée : Supabase peut répondre `400` avec `"404"` dans le corps pour un
  bucket absent ; le code reconnaît ce cas pour créer le bucket.

### 4.10 Paiement (préparé, non branché)

- Table `payments` et `payment_events`.
- `PaymentService` enregistre les tentatives et traite des notifications **déjà
  vérifiées**.
- Idempotence : contrainte unique `(provider, provider_event_id)` + vérification
  applicative ; un rejeu séquentiel ne réapplique rien.
- Vérification que le prestataire et le montant correspondent à la commande.
- Contrat `PaymentProvider` prêt pour MTN MoMo / Orange Money, **sans implémentation
  réelle** : aucun endpoint de webhook n'est exposé, et aucune implémentation
  « paiement toujours réussi » n'existe hors tests.

---

## 5. Base de données

### 5.1 Principes

- Clés primaires **UUID** générées côté application.
- Montants XAF en `bigint` ; `numeric(12,2)` uniquement pour la valeur d'une promotion.
- `timestamptz` partout, avec `created_at` / `updated_at`.
- Statuts en `varchar` + contrainte `CHECK`, mappés en `EnumType.STRING`.
- Le schéma est géré **exclusivement par Flyway** ; Hibernate est en `ddl-auto: validate`.
- Pool Hikari limité à 5 connexions (quota Supabase).
- **RLS activée sur toutes les tables**, sans politique publique : l'API REST
  auto-générée par Supabase est fermée. Le backend se connecte avec le rôle propriétaire.

### 5.2 Les 15 tables

`users`, `addresses`, `categories`, `products`, `product_images`, `promotions`,
`promotion_products`, `carts`, `cart_items`, `delivery_zones`, `orders`, `order_items`,
`payments`, `payment_events`, `shipments`.

Points clés :

- `users` : email unique normalisé en minuscules, rôles `ADMIN` / `CUSTOMER`.
- `addresses` : une seule adresse par défaut par client (index partiel).
- `products` : slug et SKU uniques, prix `bigint`, stock ≥ 0, verrou optimiste `@Version`.
- `product_images` : une image principale par produit (index partiel).
- `promotions` : pourcentage ≤ 100 ou montant fixe, fenêtre de validité ; aucun produit
  ciblé = promotion boutique entière.
- `carts` : jeton invité stocké **haché** ; unicité du panier utilisateur par index partiel.
- `orders` : vérification SQL `total = subtotal + delivery_fee - discount_amount`,
  zone et promotion tracées.
- `order_items` : prix figé, `subtotal = unit_price * quantity` vérifié en SQL.
- `payments` : référence unique, transaction prestataire unique par prestataire.
- `payment_events` : `UNIQUE(provider, provider_event_id)` = idempotence.
- `shipments` : une expédition par commande, numéro de suivi unique.

### 5.3 Migrations

| Version | Contenu |
|---|---|
| `V1` | Utilisateurs, rôles, email normalisé |
| `V2` | Catégories, produits, images |
| `V3` | Promotions et produits ciblés |
| `V4` | Paniers client/invité et lignes |
| `V5` | Adresses de livraison |
| `V6` | Zones et frais de livraison |
| `V7` | Commandes et lignes |
| `V8` | Paiements et événements idempotents |
| `V9` | Expéditions |
| `V10` | Mode de paiement de la commande rendu facultatif |

Le jeu de démonstration est une migration **répétable** (`R__demo_catalog.sql`) placée
dans `db/demo` et chargée uniquement par le profil `dev` : 6 catégories, 14 produits,
3 zones de livraison. Aucune donnée de démo en production.

### 5.4 Concurrence

| Opération | Mécanisme |
|---|---|
| Création de commande / stock | Verrou pessimiste `PESSIMISTIC_WRITE`, produits triés par id |
| Édition catalogue | Verrou optimiste `@Version` |
| Annulation | `@Version` + `lockById`, restitution groupée |
| Webhooks paiement | Contrainte unique + vérification avant application |

---

## 6. Frontend en détail

### 6.1 Routes

| Route | Rôle |
|---|---|
| `/` | Accueil, univers produits, services |
| `/boutique` | Catalogue, filtres catégorie, recherche, tri |
| `/boutique/[slug]` | Fiche produit, stock, ajout au panier |
| `/panier` | Panier, quantités, sous-total |
| `/commande` | Formulaire de commande + confirmation |
| `/suivi-commande` | Suivi par numéro + téléphone |
| `/admin` | Redirection vers `/admin/commandes` |
| `/admin/commandes` | Back-office des commandes |

### 6.2 Composants principaux

- `site-header` / `site-footer` : navigation commune, lien panier, statut API.
- `product-card`, `add-to-cart-button` : catalogue et ajout rapide.
- `cart-view` : quantités bornées au stock, retrait, récapitulatif.
- `checkout-view` : coordonnées, zone de livraison, adresse, note, total estimé,
  confirmation avec numéro de commande et lien de suivi.
- `order-tracking-view` : formulaire et frise de progression en 5 étapes.
- `admin-orders-view` : authentification Basic en mémoire, filtres, détail, actions de
  statut.
- `providers` : `QueryClientProvider` unique par session.

### 6.3 Client API et résilience

- `lib/api-client.ts` : `fetch` typé, lecture du `ProblemDetail`, variante exposant les
  en-têtes et variante `204`.
- `lib/catalog.ts` : délai maximal de 5 s et résultat tolérant aux pannes
  (`ok` / `not-found` / `unavailable`). La vitrine reste consultable avec un message clair
  si l'API est arrêtée, au lieu d'une erreur brute.
- `lib/cart.ts` : TanStack Query, jeton en `localStorage`, en-tête `X-Cart-Token`
  automatique, mise à jour optimiste du cache après mutation.
- `lib/orders.ts` : zones, création, suivi.
- `lib/admin-api.ts` : en-tête Basic construit en mémoire ; le mot de passe n'est jamais
  stocké.
- `lib/format.ts` : formatage XAF.

### 6.4 Design

- Palette et tokens Tailwind dans `globals.css` (crème, poudre, or, charbon).
- Typographies : **Cormorant Garamond** (titres) et **Manrope** (texte) via `next/font`.
- Mobile first, grilles 2 → 3 colonnes, squelettes de chargement, états vides et états
  d'erreur explicites.
- Accessibilité de base : libellés, `aria-label`, `aria-live`, fil d'Ariane.

---

## 7. Configuration et exploitation

### 7.1 Variables d'environnement

Backend (`backend/.env`, jamais commité) :

| Variable | Rôle |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `nodb`, `dev` ou `prod` |
| `SERVER_PORT` | Port HTTP (8080 par défaut) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Connexion PostgreSQL Supabase (SSL) |
| `DB_POOL_SIZE` | Taille du pool Hikari (5 par défaut) |
| `SUPABASE_URL` | URL du projet Supabase |
| `SUPABASE_SECRET_KEY` | Clé secrète serveur (contourne RLS) |
| `SUPABASE_STORAGE_BUCKET` | Bucket privé (`lisa-cosmetique`) |
| `SUPABASE_SIGNED_URL_TTL` | Durée des URLs signées (15 min) |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées, séparées par des virgules |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Amorçage du premier compte ADMIN |
| `SWAGGER_ENABLED` | Active/désactive Swagger |

Frontend (`frontend/.env.local`) : uniquement `NEXT_PUBLIC_API_URL`.

### 7.2 Séquence de démarrage

**Sans Supabase (`nodb`)** : l'API démarre sans datasource ; santé et connectivité
fonctionnent ; catalogue, panier, commande et suivi ne sont pas exposés.

**Avec Supabase (`dev`)** :
1. Flyway applique `V1`…`V10` puis la démo ;
2. le bucket privé est créé si nécessaire ;
3. le compte ADMIN est créé si les variables sont fournies ;
4. Hibernate valide le schéma sans le modifier.

**Production (`prod`)** : Swagger désactivé, HTTPS obligatoire pour HTTP Basic, migrations
et bucket au démarrage.

### 7.3 Commandes utiles

```bash
# Backend
cd backend && ./mvnw spring-boot:run       # Windows : .\mvnw.cmd spring-boot:run
cd backend && ./mvnw test

# Frontend
cd frontend && npm install
cd frontend && npm run dev
cd frontend && npm run lint && npx tsc --noEmit && npm run build
```

---

## 8. Vérifications effectuées

### 8.1 Backend — exécuté le 16 novembre 2026

```bash
cd lisa-cosmetique/backend
./mvnw test
```

**Résultat : 48 tests, 0 échec, 0 erreur, 0 test ignoré.**

| Classe de test | Tests |
|---|---:|
| `CartOrderControllerIntegrationTest` | 7 |
| `CartServiceIntegrationTest` | 4 |
| `CatalogAdminControllerIntegrationTest` | 6 |
| `CatalogControllerIntegrationTest` | 4 |
| `ProductEntityTest` | 4 |
| `HealthControllerIntegrationTest` | 4 |
| `OrderAdminControllerIntegrationTest` | 6 |
| `OrderServiceIntegrationTest` | 7 |
| `PaymentServiceIntegrationTest` | 3 |
| `SupabaseStorageServiceTest` | 3 |
| **Total** | **48** |

Ce que ces tests couvrent : lecture du catalogue et pagination, CRUD d'administration,
règles de validation produit, santé de l'API, panier invité et client, création de
commande, stock insuffisant, promotion, annulation idempotente, transitions de statut
admin, idempotence des notifications de paiement, appels Supabase Storage mockés.

### 8.2 Ce qui n'a pas pu être vérifié ici

- **Les migrations n'ont pas été exécutées sur PostgreSQL/Supabase** dans cet
  environnement (`DB_PASSWORD` vide, pas de Docker) : `RLS`, `jsonb`, index partiels et
  contraintes `CHECK` ne sont pas exercés par H2.
- Aucun test de **concurrence multi-threads** : H2 ne reproduit pas fidèlement PostgreSQL.
- Le **frontend n'a pas été reconstruit** dans le cadre de ce rapport ; les commandes
  `npm run lint`, `tsc --noEmit` et `npm run build` restent à exécuter avant livraison.
- Aucun test de bout en bout navigateur n'a été réalisé.

> Ces points sont des **limites de vérification**, pas des fonctionnalités manquantes.

---

## 9. Problèmes rencontrés et corrections apportées

1. **Fuite d'entité détachée** — `PaymentResult` renvoyait une entité `Payment`, ce qui
   provoquait une `LazyInitializationException` hors transaction. Il expose désormais un
   instantané immuable (id, référence, statut, transaction, `duplicate`).
2. **Exception non traduite** — `Payment.markSuccess` lève `IllegalArgumentException`
   quand l'identifiant de transaction manque ; le service la convertit en
   `BusinessException` (422) au lieu d'une 500.
3. **Objet SQL inutilisé** — la séquence `order_number_seq` était créée mais le numéro est
   généré côté application ; elle a été supprimée.
4. **Traçabilité des frais** — ajout de `orders.delivery_zone_id` (FK, NOT NULL) pour
   savoir quelle zone a produit les frais, indépendamment de l'expédition.
5. **Mode de paiement obligatoire alors qu'aucun paiement n'est demandé** — la migration
   `V10` rend `payment_method` facultatif ; le mode sera porté par `Payment`.
6. **Jeton de panier non lisible par le navigateur** — CORS devait exposer
   `X-Cart-Token`, sinon le frontend ne pouvait pas conserver le panier invité.
7. **Jeton invité sensible** — seul son hash SHA-256 est stocké en base.
8. **Bucket Supabase « absent » signalé en 400** — détection du `"404"` dans le corps de
   réponse pour créer le bucket au lieu d'abandonner.
9. **Supabase injoignable** — le stockage ne doit pas empêcher l'API de démarrer : les
   erreurs sont journalisées, les fonctions fichiers échouent proprement.
10. **Démarrage impossible avant Supabase** — profil `nodb` dédié pour valider la
    connectivité frontend ↔ backend sans base.
11. **Incohérence de suppression** — suppression logique des produits (historique
    préservé) et suppression physique des catégories uniquement si elles sont vides.
12. **Écran vide en cas d'API arrêtée** — le frontend renvoie des états `unavailable` avec
    messages et boutons « Réessayer » plutôt que des exceptions brutes.
13. **Connexion Supabase** — documentation du choix du **Session pooler** (port 5432) :
    le Transaction pooler (6543) est incompatible avec les requêtes préparées d'Hibernate.
14. **Double soumission de commande** — le frontend désactive le bouton pendant l'envoi ;
    le backend reste protégé par la transaction et le verrou de stock.
15. **Panier non vidé après commande** — le frontend le vide après succès, mais une erreur
    de vidage ne masque jamais la confirmation de commande.

---

## 10. Limites connues et risques

### Bloquants avant mise en production

1. **Migrations non validées sur la cible réelle** : démarrer en `dev` sur Supabase, puis
   vérifier `flyway_schema_history` et les 15 tables.
2. **Aucun contrôle d'authenticité des notifications de paiement** : `PaymentService`
   suppose une notification déjà vérifiée ; aucun endpoint de webhook ne doit être exposé
   avant l'implémentation et la vérification de signature.
3. **Doublons concurrents de webhooks** : deux transactions simultanées avec le même
   `event_id` remontent une erreur non traduite ; prévoir la capture de
   `DataIntegrityViolationException` et une réponse 200/409.
4. **Commandes non rattachées à un compte** : l'authentification client n'existe pas
   encore ; `OrderService` passe `user = null` pour un invité.

### Importants

5. Vérification de montant partielle : l'API prestataire devra aussi confirmer devise,
   référence et transaction.
6. N+1 dans l'historique des commandes (`OrderService.history`) et accès paresseux au
   produit dans `OrderResponse` ; acceptable à cette échelle, à corriger par
   `@EntityGraph`/projection si nécessaire.
7. Entités `address` et `shipment` présentes, mais pas de CRUD complet ni de suivi
   d'expédition dédié côté client.
8. Numéro de commande aléatoire et non séquentiel ; format stable et unicité garantie.
9. Panier non réservant : le stock affiché peut changer avant la validation ; la vérité
   reste la création de commande.
10. Pas de setters métier publics sur le catalogue : l'administration passe par des
    méthodes dédiées.

### Mineurs

11. `created_by` / `updated_by` restent nuls tant que l'authentification client n'existe
    pas (audit partiel).
12. L'index unique partiel `uk_carts_user` n'existe pas sous H2 ; à confirmer sur
    PostgreSQL.
13. Pas de limite au nombre d'images par produit ni de lignes par panier.
14. `OrderResponse` expose les statuts en `String` (contrat d'API stable), alors que le
    domaine utilise des enums.
15. Téléversement/suppression d'images produits non exposés (le champ `imageKey` de
    catégorie est accepté, mais aucune route d'upload).
16. Interface d'administration du **catalogue** absente côté frontend (l'API existe) ;
    seule l'administration des commandes est interfaceée.

---

## 11. Prochaines étapes recommandées

1. **Valider les migrations sur Supabase** et contrôler les contraintes, index et RLS.
2. **Authentification client JWT** (rôles `ADMIN`/`CUSTOMER`) en remplacement du HTTP
   Basic, sans modifier les contrôleurs d'administration.
3. **Intégration réelle Mobile Money** derrière `PaymentProvider` : signature des
   webhooks, vérification serveur, gestion des conflits concurrents, remboursements.
4. **Téléversement d'images produits** : routes protégées, validation MIME/taille,
   URLs signées, image principale.
5. **Interface d'administration du catalogue** et gestion des promotions/zones.
6. **Historique client** des commandes et rattachement des commandes aux comptes.
7. **Observabilité** : logs structurés, métriques, alerte sur stock bas et échecs de
   paiement.
8. **Sauvegarde et rétention** des données personnelles (adresses, téléphones).
9. **Tests de bout en bout** et validation de la concurrence sur un vrai PostgreSQL.

---

## 12. Annexes

### 12.1 Routes publiques

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/api/v1/health` | Santé de l'API |
| `GET` | `/api/v1/categories` | Catégories actives |
| `GET` | `/api/v1/products` | Produits actifs (`category`, `q`, `featured`, `page`, `size`, `sort`) |
| `GET` | `/api/v1/products/{slug}` | Fiche produit |
| `GET` | `/api/v1/delivery-zones` | Zones actives |
| `GET` | `/api/v1/cart` | Panier courant |
| `POST` | `/api/v1/cart/items` | Ajouter une ligne |
| `PATCH` | `/api/v1/cart/items/{productId}` | Modifier la quantité |
| `DELETE` | `/api/v1/cart/items/{productId}` | Retirer une ligne |
| `DELETE` | `/api/v1/cart` | Vider le panier |
| `POST` | `/api/v1/orders` | Créer une commande |
| `POST` | `/api/v1/orders/track` | Suivre une commande |
| `GET` | `/actuator/health` | Santé technique |
| `GET` | `/swagger-ui.html` | Documentation interactive |

### 12.2 Routes d'administration (`ADMIN`)

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/api/v1/admin/categories` | Toutes les catégories |
| `GET` | `/api/v1/admin/categories/{id}` | Détail |
| `POST` | `/api/v1/admin/categories` | Création |
| `PUT` | `/api/v1/admin/categories/{id}` | Mise à jour |
| `DELETE` | `/api/v1/admin/categories/{id}` | Suppression si vide |
| `GET` | `/api/v1/admin/products` | Liste paginée, inactifs inclus |
| `GET` | `/api/v1/admin/products/{id}` | Détail |
| `POST` | `/api/v1/admin/products` | Création |
| `PUT` | `/api/v1/admin/products/{id}` | Mise à jour |
| `DELETE` | `/api/v1/admin/products/{id}` | Désactivation logique |
| `GET` | `/api/v1/admin/orders` | Recherche de commandes |
| `GET` | `/api/v1/admin/orders/{id}` | Détail |
| `PATCH` | `/api/v1/admin/orders/{id}/status` | Transition de statut |

### 12.3 Statuts de commande

```
PENDING ──▶ CONFIRMED ──▶ PROCESSING ──▶ SHIPPED ──▶ DELIVERED
   │            │
   └────────────┴──▶ CANCELLED  (restitution du stock, une seule fois)
```

### 12.4 Dépannage rapide

| Symptôme | Cause probable | Action |
|---|---|---|
| L'API démarre mais le catalogue renvoie 404 | Profil `nodb` actif | Passer en `dev` après configuration Supabase |
| `Connection refused` côté frontend | API arrêtée ou mauvaise `NEXT_PUBLIC_API_URL` | Démarrer le backend, vérifier l'URL |
| Erreur de préparation des requêtes | Transaction pooler Supabase (6543) | Utiliser le Session pooler (5432) |
| Bucket introuvable | Clé secrète absente ou projet non configuré | Renseigner `SUPABASE_SECRET_KEY` |
| `401` sur `/api/v1/admin/**` | Identifiants absents/incorrects | Vérifier `ADMIN_EMAIL`/`ADMIN_PASSWORD` et le compte |
| `403` sur `/api/v1/admin/**` | Compte sans rôle `ADMIN` | Utiliser le compte administrateur |
| Erreur Flyway au démarrage | Schéma modifié manuellement | Ne jamais éditer le schéma dans Supabase ; corriger par migration |
| Images non affichées | URL signée expirée | Régénérer l'URL côté backend |

---

## 13. Conclusion

Le projet est une application e-commerce **fonctionnelle de bout en bout** pour le
périmètre « catalogue → panier → commande → suivi → back-office », avec une séparation
stricte des responsabilités, des montants calculés exclusivement côté serveur, un schéma
versionné et des tests d'intégration verts (48/48).

Ce qui reste avant une mise en production réelle relève de la **validation
d'infrastructure** (migrations Supabase), de l'**authentification client**, du
**paiement réel** et de la **gestion des images** — chantiers identifiés et préparés dans
l'architecture, mais volontairement non improvisés.
