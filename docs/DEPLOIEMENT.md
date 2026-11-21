# Déploiement — Lisa Cosmétique et Bien-être

## Vue d'ensemble

```
Navigateur ──► Vercel (frontend Next.js) ──► Render (backend Spring Boot) ──► Supabase
                https://lisa….vercel.app      https://lisa-api.onrender.com    (PostgreSQL + Storage)
```

**Vercel n'héberge pas le backend.** Vercel exécute du Next.js et des fonctions
serverless courtes ; une application Spring Boot (Java, serveur permanent, pool de
connexions, Flyway au démarrage) doit tourner ailleurs. Ce guide utilise **Render** ;
Railway, Koyeb ou Fly.io fonctionnent avec le même `Dockerfile`.

Ordre à respecter : **1. Supabase → 2. Backend → 3. Frontend → 4. CORS.**

---

## 0. Préparer le dépôt Git

Vercel et Render déploient depuis GitHub.

```bash
cd lisa-cosmetique
git init
git add .
git commit -m "Version initiale"
# Créer un dépôt PRIVÉ sur github.com, puis :
git remote add origin https://github.com/<compte>/lisa-cosmetique.git
git branch -M main
git push -u origin main
```

Vérifier **avant le premier push** qu'aucun secret n'est versionné :

- `backend/.env` et `frontend/.env.local` sont déjà ignorés par le `.gitignore` racine ;
  le confirmer avec `git status` : ils ne doivent pas apparaître.
- `backend/.env.example` contient actuellement un vrai e-mail et un vrai mot de passe
  administrateur : les remplacer par des valeurs factices, et **changer ce mot de passe**.

---

## 1. Base de données (Supabase)

Le projet Supabase existe déjà. Pour la production :

- **Option simple** : réutiliser le même projet. Le catalogue est déjà en base.
- **Option propre (recommandée)** : créer un second projet Supabase « production ».
  En profil `prod`, Flyway crée les tables mais **n'insère pas** les produits de test.
  Les vrais produits sont ensuite ajoutés par l'administration.
  Pour une base de démonstration, démarrer une fois en `dev` pour créer le catalogue de
  test, puis réimporter les photos avec `infrastructure/demo-images/upload.mjs`
  (voir `infrastructure/demo-images/CREDITS.md`).

Récupérer `DB_URL` (JDBC, *Session pooler*, port 5432, `?sslmode=require`),
`DB_USERNAME`, `DB_PASSWORD`, `SUPABASE_URL` et `SUPABASE_SECRET_KEY`
(voir [infrastructure/README.md](../infrastructure/README.md)).

---

## 2. Backend sur Render

1. Créer un compte sur https://render.com et le relier à GitHub.
2. **New → Web Service** → choisir le dépôt.
3. Réglages :

   | Champ | Valeur |
   |---|---|
   | Name | `lisa-api` |
   | Region | Frankfurt (EU Central), la plus proche du Cameroun et de Supabase eu-west |
   | Root Directory | `backend` |
   | Runtime / Language | **Docker** (le `backend/Dockerfile` est détecté) |
   | Instance Type | Free pour tester, **Starter** pour la production (voir plus bas) |
   | Health Check Path | `/actuator/health` |

4. **Environment Variables** :

   | Variable | Valeur |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | `jdbc:postgresql://aws-…pooler.supabase.com:5432/postgres?sslmode=require` |
   | `DB_USERNAME` | `postgres.<project-ref>` |
   | `DB_PASSWORD` | mot de passe Supabase |
   | `DB_POOL_SIZE` | `5` |
   | `SUPABASE_URL` | `https://<project-ref>.supabase.co` |
   | `SUPABASE_SECRET_KEY` | clé secrète Supabase |
   | `SUPABASE_STORAGE_BUCKET` | `lisa-cosmetique` |
   | `CORS_ALLOWED_ORIGINS` | provisoirement `http://localhost:3000` (mis à jour à l'étape 4) |
   | `ADMIN_EMAIL` | e-mail administrateur (uniquement au premier démarrage) |
   | `ADMIN_PASSWORD` | mot de passe **fort**, 12 caractères minimum |
   | `SWAGGER_ENABLED` | `false` |

   Ne pas définir `PORT` : Render le fournit et le `Dockerfile` l'utilise.

5. **Create Web Service**. Le premier build Maven prend 5 à 10 minutes.
6. Vérifier : `https://lisa-api.onrender.com/api/v1/health` doit répondre `"status":"UP"`.
7. Une fois connecté une première fois en administration, supprimer `ADMIN_PASSWORD`
   des variables Render (le compte reste en base).

**Offre gratuite Render** : le service s'endort après 15 minutes sans visite. Le premier
visiteur attend alors 30 à 60 secondes, et la boutique peut afficher « momentanément
indisponible ». Pour une vraie boutique, prendre l'offre **Starter** (environ 7 $/mois,
toujours allumée).

---

## 3. Frontend sur Vercel

1. Créer un compte sur https://vercel.com et le relier à GitHub.
2. **Add New → Project** → importer le dépôt.
3. Réglages :

   | Champ | Valeur |
   |---|---|
   | Framework Preset | Next.js (détecté) |
   | **Root Directory** | `frontend` |
   | Build Command | `npm run build` (par défaut) |
   | Node.js Version | 20.x ou plus (Settings → General) |

4. **Environment Variables** (Production, Preview et Development) :

   | Variable | Valeur |
   |---|---|
   | `NEXT_PUBLIC_API_URL` | `https://lisa-api.onrender.com` (sans `/` final) |

   C'est la seule variable du frontend. Ne jamais y mettre de clé Supabase : tout ce qui
   commence par `NEXT_PUBLIC_` est visible par les visiteurs.

5. **Deploy**. L'URL obtenue ressemble à `https://lisa-cosmetique.vercel.app`.

Les polices sont dans `frontend/src/assets/fonts/`, donc le build ne contacte pas
Google Fonts. Les photos produits proviennent de Supabase : si le projet Supabase de
production a une autre adresse, mettre à jour `hostname` dans `frontend/next.config.ts`.

---

## 4. Relier les deux (CORS)

Le navigateur refuse les appels du site Vercel vers Render tant que le backend ne
l'autorise pas.

Sur Render → `lisa-api` → Environment :

```
CORS_ALLOWED_ORIGINS=https://lisa-cosmetique.vercel.app,https://www.lisa-cosmetique.cm
```

Indiquer toutes les adresses exactes du site, séparées par des virgules, sans `/` final.
Render redéploie automatiquement.

---

## 5. Nom de domaine (facultatif)

- **Vercel** → Project → Settings → Domains → ajouter `lisa-cosmetique.cm` (ou autre),
  puis créer chez le registrar les enregistrements DNS indiqués.
- Optionnel : `api.lisa-cosmetique.cm` → Render (Settings → Custom Domains), puis mettre
  à jour `NEXT_PUBLIC_API_URL` sur Vercel et redéployer.
- Ajouter chaque nouveau domaine du site à `CORS_ALLOWED_ORIGINS`.

---

## 6. Vérifications après mise en ligne

1. `https://<api>/api/v1/health` → `UP`.
2. Accueil : les produits populaires s'affichent.
3. Boutique : filtres, recherche et tri fonctionnent.
4. Ajouter au panier → la pastille de l'icône panier augmente.
5. Passer une commande test → numéro de commande reçu.
6. `/suivi-commande` avec ce numéro et le téléphone → commande retrouvée.
7. `/admin/commandes` → connexion, puis faire avancer puis **annuler** la commande test.
8. `/admin/produits` → ajouter une photo, vérifier qu'elle apparaît en boutique.

Si la boutique affiche « momentanément indisponible » :

- le backend dort (offre gratuite) : patienter une minute et recharger ;
- `NEXT_PUBLIC_API_URL` est faux : corriger sur Vercel, puis **redéployer**, car la
  valeur est intégrée au build ;
- erreur CORS dans la console du navigateur : corriger `CORS_ALLOWED_ORIGINS`.

---

## 7. Mises à jour

`git push` sur `main` redéploie automatiquement Vercel et Render. Les migrations Flyway
s'appliquent seules au démarrage du backend.

## 8. Avant d'ouvrir au public

- Changer le mot de passe administrateur et le retirer de `.env.example`.
- Ne pas exposer d'endpoint de paiement tant que la vérification des notifications n'est
  pas implémentée (voir [RAPPORT_REALISATION.md](RAPPORT_REALISATION.md), section 10).
- Décider si la base de production garde ou non les produits de test.
