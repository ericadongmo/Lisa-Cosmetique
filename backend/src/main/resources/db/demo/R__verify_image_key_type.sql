-- Débogage : vérifier le type réel de la colonne image_key
SELECT
    column_name,
    data_type,
    udt_name,
    character_maximum_length
FROM information_schema.columns
WHERE table_name = 'categories'
  AND column_name = 'image_key';

-- Vérifier si les données existent
SELECT id, name, image_key FROM categories LIMIT 3;
