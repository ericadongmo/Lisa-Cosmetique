-- Correction des colonnes texte de la table products.
-- Sur certaines bases, short_description et description ont été créées en bytea,
-- ce qui casse les filtres de recherche (lower(bytea) n'existe pas dans PostgreSQL).
-- La conversion n'est faite que si la colonne est encore en bytea.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'products'
          AND column_name = 'short_description'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE products
            ALTER COLUMN short_description TYPE varchar(300)
            USING convert_from(short_description, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'products'
          AND column_name = 'description'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE products
            ALTER COLUMN description TYPE text
            USING convert_from(description, 'UTF8');
    END IF;
END
$$;
