# Base de données — modèle, migrations et Supabase

Document de référence de la phase 2. Le schéma est créé et versionné **uniquement** par
Flyway ; Hibernate est en `ddl-auto: validate` hors tests.

## 1. Principes

- Clés primaires `uuid` générées côté application.
- Montants en **francs CFA entiers** (`bigint`), aucune colonne `float`/`double`.
  Seule `promotions.discount_value` est `numeric(12,2)` car elle porte un pourcentage
  ou un montant fixe selon `discount_type`.
- Horodatages `timestamptz` avec `created_at` / `updated_at` sur toutes les tables métier.
- Statuts stockés en `varchar` avec contrainte `CHECK`, mappés en `EnumType.STRING`.
- Les prix, totaux et frais sont calculés par le backend. La table `orders` vérifie même
  en SQL que `total_amount = subtotal + delivery_fee - discount_amount`.
- L'historique est préservé : les lignes de commande figent nom, SKU et prix unitaire ;
  la commande fige l'adresse en `jsonb` ; les suppressions de produits mettent
  `product_id` à `NULL` dans les lignes de commande (`ON DELETE SET NULL`).
- RLS activée sur toutes les tables : l'API REST auto-générée par Supabase est fermée.
  Le backend se connecte avec le rôle propriétaire et n'est pas soumis à RLS.

## 2. ERD

```mermaid
erDiagram
    USERS ||--o{ ADDRESSES : possede
    USERS ||--o| CARTS : possede
    USERS ||--o{ ORDERS : passe
    CATEGORIES ||--o{ PRODUCTS : contient
    PRODUCTS ||--o{ PRODUCT_IMAGES : illustre
    PRODUCTS ||--o{ CART_ITEMS : "est ajoute"
    CARTS ||--o{ CART_ITEMS : contient
    PROMOTIONS }o--o{ PRODUCTS : cible
    DELIVERY_ZONES ||--o{ ORDERS : tarife
    DELIVERY_ZONES ||--o{ SHIPMENTS : dessert
    ORDERS ||--o{ ORDER_ITEMS : contient
    ORDERS ||--o{ PAYMENTS : "tente"
    ORDERS ||--o| SHIPMENTS : "expediee par"
    PROMOTIONS ||--o{ ORDERS : "remise appliquee"
    PAYMENTS ||--o{ PAYMENT_EVENTS : recoit

    USERS {
        uuid id PK
        varchar email UK
        varchar phone_number UK
        varchar password_hash
        varchar role
        boolean enabled
        boolean email_verified
    }
    ADDRESSES {
        uuid id PK
        uuid user_id FK
        varchar recipient_name
        varchar phone_number
        varchar city
        varchar neighborhood
        varchar street_address
        boolean is_default
    }
    CATEGORIES {
        uuid id PK
        varchar name
        varchar slug UK
        varchar image_key
        boolean active
        int position
    }
    PRODUCTS {
        uuid id PK
        uuid category_id FK
        varchar name
        varchar slug UK
        varchar sku UK
        bigint price
        bigint compare_at_price
        int stock_quantity
        int low_stock_threshold
        boolean active
        boolean featured
        bigint version
    }
    PRODUCT_IMAGES {
        uuid id PK
        uuid product_id FK
        varchar object_key UK
        int position
        boolean is_primary
    }
    PROMOTIONS {
        uuid id PK
        varchar name
        varchar discount_type
        numeric discount_value
        timestamptz start_date
        timestamptz end_date
        boolean active
    }
    CARTS {
        uuid id PK
        uuid user_id FK
        char session_token_hash UK
    }
    CART_ITEMS {
        uuid id PK
        uuid cart_id FK
        uuid product_id FK
        int quantity
    }
    DELIVERY_ZONES {
        uuid id PK
        varchar name
        varchar city
        bigint delivery_fee
        int estimated_delivery_days
        boolean active
    }
    ORDERS {
        uuid id PK
        varchar order_number UK
        uuid user_id FK
        jsonb delivery_address_snapshot
        uuid delivery_zone_id FK
        bigint subtotal
        bigint delivery_fee
        bigint discount_amount
        bigint total_amount
        varchar order_status
        varchar payment_status
        varchar payment_method
        uuid promotion_id FK
        bigint version
    }
    ORDER_ITEMS {
        uuid id PK
        uuid order_id FK
        uuid product_id FK
        varchar product_name_snapshot
        varchar product_sku_snapshot
        bigint unit_price
        int quantity
        bigint subtotal
    }
    PAYMENTS {
        uuid id PK
        uuid order_id FK
        varchar payment_reference UK
        varchar provider
        varchar payment_method
        bigint amount
        char currency
        varchar payment_status
        varchar provider_transaction_id
        timestamptz paid_at
    }
    PAYMENT_EVENTS {
        uuid id PK
        uuid payment_id FK
        varchar provider
        varchar provider_event_id
        jsonb payload
        timestamptz received_at
        timestamptz processed_at
    }
    SHIPMENTS {
        uuid id PK
        uuid order_id FK_UK
        uuid delivery_zone_id FK
        varchar tracking_number UK
        varchar shipment_status
        timestamptz shipped_at
        timestamptz delivered_at
    }
```

## 3. Tables (15)

| Table | Rôle | Points clés |
|---|---|---|
| `users` | Comptes clients et administrateurs | email unique en minuscules, rôles `ADMIN`/`CUSTOMER` |
| `addresses` | Carnet d'adresses | une seule adresse par défaut par client (index partiel) |
| `categories` | Rayons de la boutique | slug unique, ordre d'affichage, activation |
| `products` | Produits | SKU et slug uniques, prix `bigint`, stock ≥ 0, `@Version` optimiste |
| `product_images` | Photos Supabase Storage | `object_key` unique, une image principale par produit (index partiel) |
| `promotions` | Remises | pourcentage ≤ 100 ou montant fixe, fenêtre de validité |
| `promotion_products` | Produits ciblés | aucun lien = promotion boutique entière |
| `carts` | Panier client ou invité | jeton invité stocké uniquement haché (SHA-256) |
| `cart_items` | Lignes de panier | un produit par panier, quantité > 0 |
| `delivery_zones` | Tarifs de livraison | frais ≥ 0, jours estimés ≥ 0 |
| `orders` | Commandes | numéro unique, montants cohérents vérifiés en SQL, zone et remise tracées |
| `order_items` | Lignes historisées | prix figé, `subtotal = unit_price * quantity` vérifié en SQL |
| `payments` | Tentatives de paiement | référence unique, transaction prestataire unique par prestataire |
| `payment_events` | Notifications brutes | `UNIQUE(provider, provider_event_id)` = idempotence |
| `shipments` | Livraison | une expédition par commande, suivi unique |

## 4. Règles métier et garanties

| Règle | Où elle est garantie |
|---|---|
| Prix et stock non négatifs | validation Bean Validation, constructeurs, services, `CHECK` SQL |
| Quantité > 0 | `CartItem.setQuantity`, `OrderItem`, `CHECK` SQL, DTO `@Positive` |
| Prix figé dans la commande | `order_items.unit_price` + instantanés nom/SKU |
| Totaux et frais calculés backend | `OrderService` relit produits et zone ; aucun prix dans `CreateOrderRequest` |
| Stock décrémenté de façon transactionnelle | `OrderService.create` + verrou `PESSIMISTIC_WRITE` trié par `product_id` |
| Annulation restaurant le stock une seule fois | statut `CANCELLED` idempotent + verrou de commande + restitution groupée |
| Idempotence des webhooks | `payment_events` avec contrainte unique, vérification avant application |
| Conservation de l'historique | `ON DELETE SET NULL` sur les références produit, snapshots, commandes jamais supprimées |

### Concurrence

- **Stock / création de commande** : `SELECT … FOR UPDATE` sur les produits, triés par
  identifiant pour prévenir les interblocages. La décrémentation et l'enregistrement de
  la commande partagent la même transaction.
- **Catalogue** : verrou optimiste `@Version` (édition administrative à venir).
- **Commande** : `@Version` + `lockById` pour l'annulation.
- **Paiement** : la contrainte unique `(provider, provider_event_id)` est le garde-fou
  final ; un rejeu séquentiel ne réapplique rien (testé).

> Limite connue : deux notifications **simultanées** avec le même `event_id` ne sont pas
> encore converties en réponse idempotente. La seconde transaction échouera sur la
> contrainte unique ; la gestion de ce conflit (retour 200/409) appartient à la phase
> paiement, avec vérification de signature du prestataire.

## 5. Migrations Flyway

| Version | Contenu |
|---|---|
| `V1__create_users.sql` | utilisateurs, rôles, email normalisé |
| `V2__create_catalog.sql` | catégories, produits, images |
| `V3__create_promotions.sql` | promotions et produits ciblés |
| `V4__create_carts.sql` | paniers client/invité et lignes |
| `V5__create_addresses.sql` | adresses de livraison |
| `V6__create_delivery_zones.sql` | zones et frais |
| `V7__create_orders.sql` | commandes, lignes, séquence de numérotation retirée (voir revue) |
| `V8__create_payments.sql` | paiements et événements idempotents |
| `V9__create_shipments.sql` | expéditions |
| `V10__make_order_payment_method_optional.sql` | mode de paiement de la commande facultatif (choisi lors du paiement) |

Les données de démonstration sont **hors du schéma** : `db/demo/R__demo_catalog.sql`
(migration répétable, chargée uniquement par le profil `dev` via
`spring.flyway.locations`). Aucune donnée de démo n'est insérée en production.

## 6. Supabase

1. Renseigner `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SUPABASE_URL` et
   `SUPABASE_SECRET_KEY` dans `backend/.env` (voir `backend/.env.example`).
2. Utiliser le **Session pooler** (IPv4) ou la connexion directe ; le transaction pooler
   (port 6543) est incompatible avec les requêtes préparées.
3. Démarrer avec `SPRING_PROFILES_ACTIVE=dev` : Flyway applique `V1`…`V9` puis la démo.
4. Vérifier `select * from flyway_schema_history;` et la présence des 15 tables.
5. RLS est déjà activée sur chaque table, sans politique : l'API REST publique de
   Supabase ne peut lire aucune donnée. Ne jamais exposer `SUPABASE_SECRET_KEY`.
6. Le bucket de stockage reste privé ; seules les URLs signées générées par le backend
   donnent accès aux images.

## 7. Tests et limite H2

Choix retenu : **H2 en mode PostgreSQL** pour les tests locaux, sans Docker.

```bash
cd backend
./mvnw test
```

- `src/test/resources/application-test.yml` configure H2 (`MODE=PostgreSQL`),
  `ddl-auto: create-drop` et **Flyway désactivé**.
- Les tests d'intégration couvrent la création/annulation de commande, le stock, la
  promotion, le panier et l'idempotence des paiements.
- **H2 ne rejoue pas les migrations** : `ENABLE ROW LEVEL SECURITY`, `jsonb`, les index
  partiels et les contraintes `CHECK` du schéma PostgreSQL ne sont pas exercés. Les
  garanties correspondantes restent à valider sur une vraie base Supabase/PostgreSQL.
- Les migrations n'ont pas encore été exécutées : `DB_PASSWORD` est vide dans
  `backend/.env`. Aucun résultat de migration ne doit être considéré comme vérifié avant
  un démarrage réussi avec le profil `dev`.

## 8. Évolution prévue (phase 3+)

- Authentification (JWT ou sessions) et rattachement des commandes à `users`.
- CRUD d'administration du catalogue, des promotions et des zones (contrôleurs + rôles).
- Intégration réelle des prestataires Mobile Money derrière `PaymentProvider`
  (signature des webhooks, vérification serveur, gestion des conflits concurrents).
- Endpoints panier / commande / livraison et pages frontend associées.
- Stratégie de sauvegarde et de rétention des données personnelles (adresses, téléphones).
