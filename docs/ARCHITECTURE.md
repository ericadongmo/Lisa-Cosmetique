# Architecture — Lisa Cosmétique et Bien-être

## 1. Vue d'ensemble

```
 Navigateur (mobile / desktop)
        │  HTTPS
        ▼
 ┌──────────────────┐   REST JSON   ┌─────────────────────────┐   JDBC/SSL   ┌──────────────────────────┐
 │ frontend/        │ ────────────▶ │ backend/                │ ───────────▶ │ Supabase PostgreSQL      │
 │ Next.js (App     │   /api/v1/**  │ Spring Boot 3.5 / Java  │   Flyway     │ (données métier)         │
 │ Router, TS)      │ ◀──────────── │ 21                      │              ├──────────────────────────┤
 └──────────────────┘               │                         │  REST + clé  │ Supabase Storage         │
        ▲                           │                         │ ───────────▶ │ (bucket privé, images)   │
        │  GET image (URL signée, durée limitée)                             └──────────────────────────┘
        └─────────────────────────────────────────────────────────────────────────────┘
```

- Le **frontend** ne détient aucun secret : il ne connaît que l'URL publique de l'API
  (`NEXT_PUBLIC_API_URL`). Il ne parle jamais directement à Supabase.
- Le **backend** est la seule source de vérité pour les prix, stocks, frais de livraison,
  statuts de commande et paiements. Il est le seul à détenir la clé secrète Supabase.
- **Supabase Storage** stocke les fichiers. Le bucket est privé ; le backend délivre des
  URLs signées valables quelques minutes (`SUPABASE_SIGNED_URL_TTL`). Les images produits
  sont envoyées et gérées par `/admin/produits`; la vitrine ne reçoit que des URLs signées.
  Le code métier
  dépend de l'interface `StorageService`, pas de Supabase : changer de fournisseur
  (S3, MinIO…) revient à écrire une autre implémentation.
- Aucun conteneur n'est nécessaire en local : tout est hébergé par Supabase.

## 2. Backend — organisation par domaine, couches à l'intérieur

```
cm.lisacosmetique.backend
├── config/        Sécurité, CORS, OpenAPI
├── exception/     Gestion centralisée des erreurs (ProblemDetail RFC 9457)
├── health/        Endpoint de santé
├── storage/       Abstraction StorageService + implémentation Supabase Storage
├── common/audit/  Timestamps et auteurs d'audit JPA
├── catalog/       Catégories, produits, images (lecture publique)
├── user/          Comptes et rôles
├── address/       Adresses de livraison
├── promotion/     Remises et produits ciblés
├── cart/          Paniers client et invité
├── order/         Commandes, stock, lignes, suivi public et back-office
├── payment/       Paiements et événements idempotents
├── delivery/      Zones de livraison et expéditions
└── (à venir)      auth/ (JWT client), paiement réel, notifications
       └── chaque domaine : controller / service / repository / entity / dto / mapper
```

Le code est regroupé **par domaine métier** (catalogue, commande…) plutôt que par couche
technique globale : chaque domaine contient ses propres Controller → Service → Repository →
Entity, avec DTO et Mapper. Les entités JPA ne sortent jamais d'un service : l'API
n'expose que des DTO.

### Profils Spring

| Profil  | Usage                                                        |
|---------|--------------------------------------------------------------|
| `nodb`  | Démarrage sans base, tant que Supabase n'est pas configuré (catalogue non exposé) |
| `dev`   | Développement avec Supabase (PostgreSQL + Storage)           |
| `prod`  | Production (Swagger désactivé par défaut)                    |
| `test`  | Tests automatisés (valeurs factices, aucun service externe)  |

### Base de données

- Schéma géré **uniquement** par Flyway (`src/main/resources/db/migration/V<n>__<description>.sql`).
- Hibernate en `ddl-auto: validate` : il vérifie le schéma mais ne le modifie jamais.
- Pool Hikari limité (5 par défaut) à cause des quotas de connexions Supabase.
- Connexion via le *Session pooler* Supabase (IPv4) ou la connexion directe ;
  le *Transaction pooler* (port 6543) est incompatible avec les requêtes préparées.
- Supabase expose automatiquement le schéma `public` via son API REST : chaque migration
  qui crée une table y active RLS sans politique publique. Le backend, connecté avec le
  rôle propriétaire, n'est pas concerné ; l'accès direct depuis Internet est bloqué.

### Catalogue

- Routes publiques : `GET /api/v1/categories`, `GET /api/v1/products`,
  `GET /api/v1/products/{slug}`. Seuls les produits `active` sont exposés ; un produit
  inconnu ou inactif renvoie un `404` au format ProblemDetail.
- `GET /api/v1/products` accepte `category` (slug), `q` (nom / description courte),
  `featured`, `page`, `size` (max 48) et `sort` (`name`, `price-asc`, `price-desc`,
  `newest`). Le tri est une liste fermée : aucun nom de champ arbitraire n'atteint la base.
- Les prix sont des entiers en FCFA (`price_xaf`, `bigint`), jamais des décimaux.
- Schéma créé par Flyway (`V2__create_catalog.sql`) avec RLS activée ; le jeu de
  démonstration est séparé dans `db/demo` et chargé uniquement par le profil `dev`.
- Modèle complet, migrations et règles de concurrence : voir [DATABASE.md](DATABASE.md).
- Administration du catalogue disponible sur `/api/v1/admin/**` (rôle `ADMIN`) :
  CRUD produits/catégories, suppression logique des produits. Voir
  [ADMIN_CATALOG.md](ADMIN_CATALOG.md). Les images produits restent à brancher.

### Stockage

- Activé uniquement si `SUPABASE_SECRET_KEY` est renseignée (sinon aucun bean
  `StorageService` : l'API démarre quand même).
- Bucket privé créé au démarrage s'il n'existe pas.
- Les clés d'objets sont générées côté serveur
  (ex. `products/<product-id>/<uuid>.webp`), avec contrôle du format réel du fichier
  et de la taille (5 Mo maximum).

### Sécurité (état actuel)

- API stateless, CSRF désactivé (pas de cookie de session), formulaire de connexion désactivé.
- HTTP Basic activé pour l'administration, adossé à la table `users` (BCrypt).
- Routes publiques : `/api/v1/health`, `/actuator/health`, Swagger, lecture du catalogue.
- `/api/v1/admin/**` réservé au rôle `ADMIN` ; tout le reste exige une authentification.
- Premier compte ADMIN amorcé par `ADMIN_EMAIL` / `ADMIN_PASSWORD` s'il n'existe pas.
- CORS : liste explicite d'origines (`CORS_ALLOWED_ORIGINS`), jamais `*`.
- Réponses d'erreur sans trace technique.

## 3. Décisions prévues pour les étapes suivantes

### Authentification
JWT signé par le backend (rôles `ADMIN`, `CUSTOMER`) en remplacement du HTTP Basic
actuel. Les mots de passe sont déjà hachés en BCrypt et le compte administrateur initial
est créé par une procédure explicite (`ADMIN_EMAIL` / `ADMIN_PASSWORD`), sans donnée
codée en dur. L'ajout du login JWT ne modifiera pas les contrôleurs d'administration.

### Paiements — abstraction `PaymentProvider`

```java
public interface PaymentProvider {
    PaymentMethod method();                         // MTN_MOMO, ORANGE_MONEY, CASH_ON_DELIVERY
    PaymentInitiation initiate(Order order);        // demande au prestataire
    PaymentStatus verify(String providerReference); // interrogation côté serveur
    WebhookResult handleWebhook(HttpHeaders headers, byte[] body); // signature vérifiée
}
```

Règles :
- Une commande ne passe en `CONFIRMED` (paiement mobile) qu'après confirmation serveur
  du prestataire (webhook à signature vérifiée **et/ou** appel de vérification).
- Aucune implémentation « paiement toujours réussi » n'est chargée hors du profil `test`.
- Le paiement à la livraison crée une commande `PENDING`, confirmée manuellement par l'admin.

### Commandes
Statuts : `PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED`, et `CANCELLED`.
Le montant est **recalculé côté serveur** (prix en base, promotions actives, stock,
frais de livraison de la zone) ; les montants envoyés par le client sont ignorés.
Montants stockés en entiers (FCFA, XAF, sans décimales).

### Livraison
Zones configurables (Yaoundé, autres villes) avec frais par zone, et option
« retrait en boutique » activable dans les paramètres.

## 4. Frontend

```
frontend/src
├── app/            Routes : /, /boutique, /panier, /commande, /suivi-commande, /admin
├── components/     Composants réutilisables (en-tête, pied de page, carte produit…)
├── lib/            Client API, types et lectures du catalogue, formatage des prix
└── (à venir) features/<domaine>/ : hooks TanStack Query, schémas Zod, stores Zustand
```

- TanStack Query pour toutes les données distantes, Zustand pour le panier local
  (le panier reste revérifié par le backend à la commande).
- React Hook Form + Zod pour les formulaires.
- Mobile first ; palette et typographies déclarées comme tokens Tailwind dans `globals.css`.
