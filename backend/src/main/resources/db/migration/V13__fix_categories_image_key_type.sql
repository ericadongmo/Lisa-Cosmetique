-- Correction du type de la colonne image_key dans la table categories.
-- V2 créait image_key en varchar(512), mais la migration a créé bytea.
ALTER TABLE categories
    ALTER COLUMN image_key TYPE varchar(512);
