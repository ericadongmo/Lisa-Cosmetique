# Administration du catalogue

## 1. Authentification

Les routes `/api/v1/admin/**` exigent le rôle `ADMIN`. L'authentification actuelle est
**HTTP Basic** (à utiliser exclusivement derrière HTTPS) et s'appuie sur la table `users`
(mots de passe BCrypt). JWT reste prévu pour une étape ultérieure ; les contrôleurs n'auront
pas à changer.

Créer le premier compte au démarrage du backend :

```dotenv
# backend/.env
ADMIN_EMAIL=admin@exemple.cm
ADMIN_PASSWORD=un-mot-de-passe-de-8-caracteres-minimum
```

- Le compte est créé uniquement s'il n'existe pas ; un compte existant n'est jamais modifié.
- Retirer ensuite ces deux variables de l'environnement.
- En production, ne jamais commiter de mot de passe ; utiliser les variables du serveur.

Exemple :

```bash
curl -u admin@exemple.cm:mot-de-passe http://localhost:8080/api/v1/admin/products
```

| Situation | Réponse |
|---|---|
| Aucun identifiant | `401 Unauthorized` |
| Compte `CUSTOMER` | `403 Forbidden` |
| Compte `ADMIN` | accès autorisé |

## 2. Routes

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/api/v1/admin/categories` | liste toutes les catégories, désactivées incluses |
| `GET` | `/api/v1/admin/categories/{id}` | détail |
| `POST` | `/api/v1/admin/categories` | création (`201`) |
| `PUT` | `/api/v1/admin/categories/{id}` | mise à jour complète |
| `DELETE` | `/api/v1/admin/categories/{id}` | suppression d'une catégorie vide (`204`) |
| `GET` | `/api/v1/admin/products?q=&categoryId=&active=&page=&size=` | liste paginée, inactifs inclus |
| `GET` | `/api/v1/admin/products/{id}` | détail |
| `POST` | `/api/v1/admin/products` | création (`201`) |
| `PUT` | `/api/v1/admin/products/{id}` | mise à jour complète |
| `DELETE` | `/api/v1/admin/products/{id}` | désactivation logique (`204`) |
| `GET` | `/api/v1/admin/products/{id}/images` | liste les images et leurs URLs temporaires |
| `POST` | `/api/v1/admin/products/{id}/images` | envoie une image multipart (`file`, `altText`, `primary`) |
| `DELETE` | `/api/v1/admin/products/{id}/images/{imageId}` | supprime une image (`204`) |

## 3. Corps des requêtes

Catégorie :

```json
{
  "name": "Nouveautés",
  "slug": "nouveautes",
  "description": "Les derniers arrivages",
  "imageKey": null,
  "position": 1,
  "active": true
}
```

Produit :

```json
{
  "categoryId": "11111111-1111-1111-1111-111111111101",
  "name": "Sérum éclat",
  "slug": "serum-eclat",
  "sku": "LC-COS-001",
  "shortDescription": "Un teint lumineux",
  "description": "Description complète",
  "price": 12500,
  "compareAtPrice": 15000,
  "stockQuantity": 18,
  "lowStockThreshold": 3,
  "active": true,
  "featured": false
}
```

- `slug` : minuscules, chiffres et tirets uniquement.
- `price`, `compareAtPrice` : XAF entiers ; le prix barré doit être strictement supérieur.
- `active` absent à la création vaut `true`. `featured` absent vaut `false`.
- Le prix et le stock sont modifiables par l'admin ; la création de commande reste la
  seule opération qui réserve du stock de façon concurrente.

## 4. Images produits

La page `/admin/produits` permet de choisir un produit, d'envoyer une image JPEG, PNG ou
WebP (5 Mo maximum), de renseigner son texte alternatif et de la définir comme image
principale. Les images sont conservées dans le bucket privé `lisa-cosmetique`. Le backend
génère les clés d'objets et les URLs de consultation signées ; la clé Supabase ne quitte
jamais le backend. La première image envoyée devient principale automatiquement.

Les images principales apparaissent sur les cartes du catalogue. La fiche produit affiche
la galerie complète. Lorsqu'une image principale est supprimée, la première image restante
devient principale.

## 4. Règles de suppression

- **Produit** : la suppression est **logique** (`active = false`). Le produit disparaît du
  catalogue public mais reste consultable en administration ; les lignes de commande
  historiques conservent nom, SKU et prix.
- **Catégorie** : suppression physique autorisée uniquement si elle ne contient aucun
  produit, sinon `409 Conflict`. Pour vider une catégorie, déplacer ses produits via
  `PUT /api/v1/admin/products/{id}`.
- Désactiver une catégorie la retire de la liste publique des catégories ; ses produits
  restent exposés tant qu'ils sont actifs.

## 5. Erreurs

Format RFC 9457 (`application/problem+json`) :

| Statut | Cas |
|---|---|
| `400` | validation (`slug` invalide, prix négatif, stock négatif, champ manquant) |
| `401` | authentification absente ou incorrecte |
| `403` | compte authentifié sans rôle `ADMIN` |
| `404` | produit ou catégorie inconnu |
| `409` | slug/SKU déjà utilisé, suppression d'une catégorie non vide |
| `422` | prix barré incohérent |

## 6. Non couvert dans cette étape

- Téléversement et suppression des images produits (le champ `imageKey` de catégorie est
  accepté, mais aucune route d'upload n'existe encore).
- Gestion des comptes administrateurs et connexion JWT.
- Interface frontend d'administration.
