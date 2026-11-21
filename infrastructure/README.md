# Infrastructure — Supabase

Le projet n'utilise pas de conteneurs locaux : la base PostgreSQL et le stockage des
fichiers sont hébergés par Supabase.

## 1. Créer le projet

1. Créer un compte sur https://supabase.com puis un nouveau projet.
2. Région : choisir la plus proche des clients (ex. Europe de l'Ouest pour le Cameroun).
3. Conserver le mot de passe de la base dans un gestionnaire de mots de passe.

## 2. Récupérer les valeurs pour `backend/.env`

| Variable              | Où la trouver dans le dashboard                                         |
|-----------------------|-------------------------------------------------------------------------|
| `SUPABASE_URL`        | Project Settings → API : *Project URL*                                  |
| `SUPABASE_SECRET_KEY` | Project Settings → API Keys : *Secret key* (ou ancienne *service_role*) |
| `DB_URL`              | Bouton **Connect** → JDBC → *Session pooler*, préfixé `jdbc:`, + `?sslmode=require` |
| `DB_USERNAME`         | Même écran : `postgres.<project-ref>`                                   |
| `DB_PASSWORD`         | Mot de passe choisi à la création du projet                             |

Puis `SPRING_PROFILES_ACTIVE=dev`.

## 3. Règles de sécurité

- La clé secrète ne doit **jamais** apparaître dans le frontend ni dans Git.
- Le navigateur ne parle jamais directement à Supabase : toutes les opérations passent
  par l'API Spring Boot, qui applique les rôles et les validations.
- Le bucket `lisa-cosmetique` est créé **privé** par le backend au premier démarrage.
  Ne pas le rendre public : les images sont servies par URLs signées à durée limitée.
- Le schéma de la base est géré uniquement par les migrations Flyway du backend ;
  ne pas créer de tables à la main dans l'éditeur SQL de Supabase.
- Les tables créées par Flyway dans le schéma `public` sont exposées par défaut à l'API
  REST automatique de Supabase : une migration activera RLS sur chaque table (sans
  politique d'accès publique) pour bloquer cet accès direct.
