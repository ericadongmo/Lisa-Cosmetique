-- Correction du type de la colonne session_token_hash pour les bases
-- où V4 a été appliquée avec char(64) au lieu de varchar(64).
-- Uniquement si la colonne est encore de type bpchar/char. PostgreSQL ne
-- change rien si la colonne est déjà varchar(64) (pg_typeof reste varchar).
ALTER TABLE carts
    ALTER COLUMN session_token_hash TYPE varchar(64);
