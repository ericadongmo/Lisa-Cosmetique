-- Correction du type de la colonne currency dans la table payments.
-- V8 créait currency en char(3) au lieu de varchar(3).
ALTER TABLE payments
    ALTER COLUMN currency TYPE varchar(3);
