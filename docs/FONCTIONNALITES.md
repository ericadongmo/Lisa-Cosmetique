# Fonctionnalités — Lisa Cosmétique et Bien-être

État au 21 novembre 2026.

## 1. Côté client (vitrine publique)

### Navigation et identité
- En-tête noir fixe : logo « LISA », liens Boutique et Suivi, **icône panier** avec pastille
  rose indiquant le nombre d'articles. Sur mobile, les liens deviennent des icônes.
- Barre de contact au-dessus du menu : **+237 699 94 10 22** (appel direct au toucher) et
  **Ancien dépôt Guinness** (ouvre Google Maps).
- Pied de page : coordonnées, lien WhatsApp, liens rapides (produits, suivi, panier).
- Charte **noir, rose et or** ; polices Montserrat (titres) et Poppins (texte), hébergées
  dans le projet (aucune dépendance à Google Fonts).

### Accueil (`/`)
- Bandeau d'accroche et lien vers la boutique.
- **Produits populaires** : jusqu'à 8 produits mis en avant (`featured`), les plus récents d'abord.
- « Nos univers » : accès direct aux 6 catégories.
- Arguments de service (livraison, commande sans compte, suivi en ligne).

### Boutique (`/boutique`)
- Catalogue paginé des produits actifs.
- Filtre par **catégorie** : produits cosmétiques, laits et soins corporels, perruques,
  mèches et extensions, accessoires de beauté, entretien capillaire.
- **Recherche** par nom ou description courte.
- **Tri** : nom, prix croissant, prix décroissant, nouveautés.
- Carte produit : photo (ou visuel de remplacement), catégorie, nom, description,
  **prix promo en rose + ancien prix barré + badge « −20 % »**, disponibilité
  (« En stock » / « Épuisé »), bouton « Ajouter au panier ».

### Fiche produit (`/boutique/[slug]`)
- Galerie de photos, description complète, prix (avec promotion éventuelle),
  stock disponible, ajout au panier.

### Panier (`/panier`)
- Panier **invité**, sans création de compte (jeton `X-Cart-Token` conservé dans le navigateur).
- Modification des quantités (limitée au stock), suppression d'un article, sous-total.

### Commande (`/commande`)
- Formulaire : nom, téléphone, e-mail facultatif, zone de livraison, quartier,
  rue / point de repère, complément, note.
- Zones et frais de livraison : Yaoundé centre (1 500 FCFA, 1 jour), Douala
  (3 000 FCFA, 2 jours), autres villes (4 000 FCFA, 3 jours).
- **Pas de paiement en ligne** : le règlement est convenu avec la boutique.
- À la validation : contrôle du stock, décrément du stock, numéro de commande unique,
  page de confirmation récapitulative.

### Suivi de commande (`/suivi-commande`)
- Recherche par **numéro de commande + numéro de téléphone** (les deux sont exigés pour
  protéger les données).
- Frise d'avancement : En attente → Confirmée → En préparation → Expédiée → Livrée
  (ou Annulée).

## 2. Côté boutique (administration)

Accès par e-mail et mot de passe administrateur (HTTP Basic ; identifiants gardés
uniquement dans la mémoire de l'onglet).

### Commandes (`/admin/commandes`)
- Liste paginée, filtre par statut, recherche (numéro, nom, téléphone).
- Détail : client, adresse, articles, sous-total, livraison, remise, total, paiement.
- Faire avancer une commande : confirmer → préparer → marquer comme expédiée (avec numéro
  de suivi facultatif) → marquer comme livrée.
- Annulation avec **restitution automatique du stock**.

### Images des produits (`/admin/produits`)
- Envoi de photos (JPEG, PNG, WebP, 5 Mo max), texte alternatif, choix de la photo principale.
- Suppression d'images. Stockage privé Supabase, affichage par liens signés temporaires.

### Catalogue (API uniquement, pas encore d'écran)
- Création, modification et désactivation de produits et de catégories via
  `/api/v1/admin/products` et `/api/v1/admin/categories` (Swagger en développement).

## 3. Produits de test

En profil `dev`, deux migrations Flyway répétables remplissent le catalogue :

| Fichier | Contenu |
|---|---|
| `backend/src/main/resources/db/demo/R__demo_catalog.sql` | 6 catégories, 14 produits, 3 zones de livraison |
| `backend/src/main/resources/db/demo/R__demo_catalog_extra.sql` | 16 produits supplémentaires |

Les produits supplémentaires couvrent volontairement des cas à tester :

| Cas | Produit(s) |
|---|---|
| Promotion (prix barré) | Rouge à lèvres mat, palette 12 teintes, perruque bob, fer à lisser |
| Rupture de stock | Savon noir africain (« Épuisé », bouton désactivé) |
| Stock faible | Crème mains karité & amande (2), perruque body wave (2) |
| Produit masqué | Spray brillance (inactif : absent de la boutique, visible en admin) |
| Mis en avant | Mascara, huile au monoï, mèches X-pression, masque à l'avocat… |

Total : **29 produits visibles** en boutique (30 en base, dont 1 masqué).
Ces migrations ne s'exécutent **pas** en profil `prod`.

**Photos** : 26 produits sur 29 ont une photo libre de droits (CC0 / domaine public),
stockée dans Supabase. Sans photo : crème mains karité & amande, perruque lisse 100 %
naturelle, savon noir africain (à compléter depuis `/admin/produits`). Les fichiers, le
script d'import et les crédits sont dans `infrastructure/demo-images/`. Ce sont des photos
génériques : les remplacer par les vraies photos des produits avant l'ouverture.

## 4. Pas encore disponible

- Comptes clients (inscription, historique de commandes).
- Paiement en ligne (Mobile Money, carte) : l'architecture est prête, aucun prestataire branché.
- Écran d'administration du catalogue (prix, stock, création de produits).
- Codes promo saisis par le client (le moteur de promotions existe côté serveur).
- Notifications SMS / WhatsApp / e-mail automatiques.
