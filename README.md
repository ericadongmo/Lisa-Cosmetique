# Lisa Cosmétique et Bien-être — plateforme e-commerce

Boutique en ligne (Cameroun) : catalogue, panier, commandes, suivi de commande et espace
d'administration.

| Dossier           | Contenu                                                           |
|-------------------|-------------------------------------------------------------------|
| `frontend/`       | Next.js 16 (App Router), TypeScript, Tailwind CSS 4               |
| `backend/`        | Spring Boot 3.5, Java 21, Maven (wrapper inclus)                  |
| `infrastructure/` | Configuration des services hébergés (Supabase)                    |
| `docs/`           | Documentation — voir [ARCHITECTURE](docs/ARCHITECTURE.md) et [DATABASE](docs/DATABASE.md) |

Services externes : **Supabase** fournit à la fois la base PostgreSQL et le stockage des
fichiers (Supabase Storage). Aucun Docker n'est nécessaire.

## Prérequis

- Node.js ≥ 20.9 et npm
- JDK 21 (ex. Eclipse Temurin). Maven n'est pas nécessaire : utiliser le wrapper `mvnw`.
- Un projet Supabase (gratuit pour démarrer) — voir [infrastructure/README.md](infrastructure/README.md)

## Installation

```bash
# Variables d'environnement (aucun de ces fichiers n'est commité)
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env.local

# Dépendances frontend
cd frontend && npm install
```

## Démarrage

```bash
# Backend — http://localhost:8080
cd backend
./mvnw spring-boot:run          # Windows PowerShell : .\mvnw.cmd spring-boot:run

# Frontend — http://localhost:3000 (dans un second terminal)
cd frontend
npm run dev
```

La page d'accueil affiche dans l'en-tête l'état de la connexion à l'API.

### Avant la création du projet Supabase

Avec `SPRING_PROFILES_ACTIVE=nodb` et `SUPABASE_SECRET_KEY` vide dans `backend/.env`,
l'API démarre sans base de données ni stockage : cela suffit pour valider la
communication frontend ↔ backend.

### Après la création du projet Supabase

Renseigner `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
dans `backend/.env`, passer `SPRING_PROFILES_ACTIVE=dev`, puis relancer le backend :
Flyway applique les migrations et le bucket privé est créé automatiquement.

## URLs utiles

| URL                                          | Rôle                          |
|----------------------------------------------|-------------------------------|
| http://localhost:3000/                       | Accueil                       |
| http://localhost:3000/boutique               | Catalogue produits            |
| http://localhost:3000/panier                 | Panier                        |
| http://localhost:3000/commande               | Formulaire de commande        |
| http://localhost:3000/suivi-commande         | Suivi de commande             |
| http://localhost:3000/admin/commandes        | Administration des commandes  |
| http://localhost:8080/api/v1/health          | Santé de l'API (public)       |
| http://localhost:8080/api/v1/categories      | Catégories (public)           |
| http://localhost:8080/api/v1/products        | Produits actifs (public)      |
| http://localhost:8080/api/v1/delivery-zones  | Zones de livraison (public)   |
| http://localhost:8080/api/v1/cart            | Panier courant (public)       |
| http://localhost:8080/api/v1/orders          | Création de commande (public) |
| http://localhost:8080/api/v1/orders/track    | Suivi de commande (public)    |
| http://localhost:8080/api/v1/admin/orders    | Commandes (rôle `ADMIN`)      |
| http://localhost:8080/actuator/health        | Santé technique (public)      |
| http://localhost:8080/swagger-ui.html        | Documentation Swagger         |
| http://localhost:8080/api/v1/admin/products  | Administration (rôle `ADMIN`) |

Le premier démarrage avec le profil `dev` applique les migrations Flyway puis le jeu de
démonstration séparé (`db/demo`, jamais chargé en production). Avec le profil `nodb`,
l'API démarre sans base : catalogue, panier, commande et suivi ne sont pas exposés.

## Administration du catalogue

Renseigner `ADMIN_EMAIL` et `ADMIN_PASSWORD` (8 caractères minimum) dans `backend/.env`,
puis redémarrer : le premier compte `ADMIN` est créé s'il n'existe pas. L'API
d'administration utilise HTTP Basic (HTTPS obligatoire en production) et le rôle `ADMIN` :

```bash
curl -u admin@exemple.cm:mot-de-passe http://localhost:8080/api/v1/admin/products
```

Routes, corps de requêtes et règles de suppression : [docs/ADMIN_CATALOG.md](docs/ADMIN_CATALOG.md).

## Administration des commandes

La page `/admin/commandes` permet de rechercher les commandes, d'afficher leurs
coordonnées, de les confirmer, de les préparer, de les expédier (avec numéro de suivi
facultatif), de les marquer livrées ou de les annuler. Une annulation restitue le stock
une seule fois. Chaque changement est immédiatement visible dans le suivi public.

L'interface demande les identifiants `ADMIN_EMAIL` / `ADMIN_PASSWORD` et ne les conserve
qu'en mémoire dans l'onglet courant. L'API reste protégée par HTTP Basic ; elle doit donc
être servie en HTTPS en production.

```bash
# Liste filtrée
curl -u admin@exemple.cm:mot-de-passe \
  'http://localhost:8080/api/v1/admin/orders?status=PENDING&q=Awa'

# Confirmer, expédier puis livrer
curl -u admin@exemple.cm:mot-de-passe -X PATCH \
  -H "Content-Type: application/json" -d '{"status":"CONFIRMED"}' \
  http://localhost:8080/api/v1/admin/orders/<uuid>/status
curl -u admin@exemple.cm:mot-de-passe -X PATCH \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED","trackingNumber":"TRACK-001"}' \
  http://localhost:8080/api/v1/admin/orders/<uuid>/status
curl -u admin@exemple.cm:mot-de-passe -X PATCH \
  -H "Content-Type: application/json" -d '{"status":"DELIVERED"}' \
  http://localhost:8080/api/v1/admin/orders/<uuid>/status
```

Transitions autorisées : `PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED`, plus
`CANCELLED` depuis `PENDING` ou `CONFIRMED`. Une commande livrée ou annulée est close.

## Panier et commande

Le panier invité est identifié par un jeton opaque émis par l'API dans l'en-tête
`X-Cart-Token` ; le frontend le conserve en `localStorage`. Les prix, stocks et frais de
livraison sont toujours recalculés côté backend, et la commande décrémente le stock dans
la même transaction. Aucun mode de paiement n'est demandé à la création : la commande
reste `PENDING` et le règlement est convenu avec la boutique.

Le suivi public exige le numéro de commande **et** le téléphone utilisé. La réponse
n'expose ni l'adresse, ni l'e-mail, ni le téléphone ; en cas de discordance, l'API renvoie
uniformément `404`.

```bash
# Panier (sans jeton : l'API en renvoie un dans X-Cart-Token)
curl -i http://localhost:8080/api/v1/cart
curl -H "X-Cart-Token: <jeton>" -H "Content-Type: application/json" \
  -d '{"productId":"<uuid>","quantity":1}' http://localhost:8080/api/v1/cart/items

# Commande
curl -H "Content-Type: application/json" -d @commande.json http://localhost:8080/api/v1/orders

# Suivi (numéro + téléphone, indicatif et espaces acceptés)
curl -H "Content-Type: application/json" \
  -d '{"orderNumber":"LC-20261113-ABC123","phone":"+237 6 90 00 00 00"}' \
  http://localhost:8080/api/v1/orders/track
```

## Tests

```bash
cd backend && ./mvnw test
cd frontend && npm run lint && npx tsc --noEmit && npm run build
```

Les tests JPA utilisent H2 en mode PostgreSQL sans Docker : ils ne rejouent pas les
migrations Flyway (RLS, `jsonb`, index partiels). Voir
[docs/DATABASE.md](docs/DATABASE.md#7-tests-et-limite-h2).

## Sécurité

- Aucun secret dans Git : seuls les fichiers `.env.example` sont versionnés.
- Le frontend ne reçoit que `NEXT_PUBLIC_API_URL` : ni identifiants de base ni clé Supabase.
- La clé secrète Supabase (qui contourne RLS) n'existe que dans `backend/.env`.
- Prix, stocks, frais de livraison et remises sont vérifiés exclusivement par le backend.
- Le suivi de commande n'est accessible qu'avec le couple numéro + téléphone.
- L'interface admin ne stocke pas les identifiants : ils restent en mémoire de l'onglet.
- Bucket de stockage privé, fichiers accessibles uniquement par URLs signées à durée limitée.
