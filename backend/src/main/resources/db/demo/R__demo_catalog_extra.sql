-- Produits de test supplémentaires (profil dev uniquement, comme R__demo_catalog.sql).
-- Migration répétable : ré-exécutée quand son contenu change, sans doublon (clé = id).
--
-- Cas couverts pour les tests :
--   * promotions (compare_at_price > price)        → prix barré
--   * stock faible (stock <= low_stock_threshold)  → alerte côté admin
--   * rupture (stock = 0)                           → « Épuisé », bouton désactivé
--   * produit inactif (active = false)              → invisible en boutique, visible en admin
--   * produits mis en avant (featured = true)       → section « Produits populaires » de l'accueil
insert into products (id, category_id, name, slug, short_description, description, sku, price, compare_at_price, stock_quantity, low_stock_threshold, active, featured) values
    -- Produits cosmétiques
    ('22222222-2222-2222-2222-222222222215', '11111111-1111-1111-1111-111111111101', 'Rouge à lèvres mat longue tenue', 'rouge-levres-mat', 'Couleur intense, tenue 12 h', 'Rouge à lèvres mat enrichi en vitamine E, qui ne dessèche pas les lèvres. Teinte rouge cerise, application précise grâce à sa pointe biseautée. Tube 3,5 g.', 'LC-COS-004', 6000, 7500, 22, 4, true, true),
    ('22222222-2222-2222-2222-222222222216', '11111111-1111-1111-1111-111111111101', 'Mascara volume intense', 'mascara-volume-intense', 'Cils allongés et recourbés', 'Mascara noir à brosse incurvée, effet volume dès la première couche. Formule résistante à la chaleur et à l''humidité. Tube 9 ml.', 'LC-COS-005', 5500, null, 18, 4, true, true),
    ('22222222-2222-2222-2222-222222222217', '11111111-1111-1111-1111-111111111101', 'Palette fards à paupières 12 teintes', 'palette-fards-12-teintes', 'Nudes, bronzes et touches dorées', 'Palette de douze fards mats et irisés, très pigmentés, adaptés aux peaux mates et foncées. Miroir intégré.', 'LC-COS-006', 11000, 13500, 9, 3, true, false),

    -- Laits et soins corporels
    ('22222222-2222-2222-2222-222222222218', '11111111-1111-1111-1111-111111111102', 'Savon noir africain', 'savon-noir-africain', 'Nettoie et purifie en douceur', 'Savon noir traditionnel à l''huile de palme et au beurre de karité. Convient au visage et au corps, idéal pour les peaux à imperfections. Pain 200 g.', 'LC-COR-004', 3000, null, 0, 5, true, false),
    ('22222222-2222-2222-2222-222222222219', '11111111-1111-1111-1111-111111111102', 'Huile corporelle sèche au monoï', 'huile-seche-monoi', 'Peau satinée, parfum solaire', 'Huile sèche non grasse au monoï et à l''huile de coco. Pénètre instantanément et laisse un voile légèrement nacré. Spray 100 ml.', 'LC-COR-005', 7500, null, 15, 4, true, true),
    ('22222222-2222-2222-2222-222222222220', '11111111-1111-1111-1111-111111111102', 'Crème mains karité & amande', 'creme-mains-karite-amande', 'Répare les mains sèches', 'Crème mains à absorption rapide au karité et à l''huile d''amande douce. Format pochette, à emporter partout. Tube 75 ml.', 'LC-COR-006', 3500, null, 2, 3, true, false),

    -- Perruques
    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111103', 'Perruque bob carré lisse', 'perruque-bob-carre', 'Coupe courte, prête à porter', 'Perruque bob en cheveux naturels, coupe carrée au niveau du menton, lace frontale 13x4. Densité 150 %, longueur 30 cm.', 'LC-PER-003', 55000, 65000, 5, 2, true, true),
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111103', 'Perruque body wave 70 cm', 'perruque-body-wave', 'Ondulations amples et naturelles', 'Perruque longue en cheveux naturels body wave, lace frontale 13x4, densité 180 %. Peut être colorée et lissée.', 'LC-PER-004', 98000, null, 2, 2, true, false),

    -- Mèches et extensions
    ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111104', 'Mèches deep wave — 3 bundles', 'meches-deep-wave-3-bundles', 'Boucles serrées et définies', 'Lot de trois bundles de cheveux naturels deep wave, longueur 50 cm, couleur naturelle (1B). Boucles durables après lavage.', 'LC-MEC-003', 70000, null, 4, 2, true, false),
    ('22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111104', 'Mèches à tresser X-pression (lot de 6)', 'meches-tresser-xpression', 'Pour box braids et twists', 'Lot de six paquets de mèches synthétiques pré-étirées, légères et sans démangeaisons. Couleur noire (1). Longueur 120 cm dépliée.', 'LC-MEC-004', 4000, null, 50, 10, true, true),

    -- Accessoires de beauté
    ('22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111105', 'Bonnet en satin', 'bonnet-satin', 'Protège les cheveux la nuit', 'Bonnet double épaisseur en satin, élastique doux et réglable. Limite les frottements et la casse. Taille unique.', 'LC-ACC-003', 3000, null, 35, 8, true, false),
    ('22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111105', 'Fer à lisser céramique', 'fer-lisser-ceramique', 'Plaques céramique, 230 °C', 'Fer à lisser à plaques flottantes en céramique, température réglable de 150 à 230 °C, chauffe en 30 secondes. Arrêt automatique.', 'LC-ACC-004', 25000, 30000, 7, 2, true, true),
    ('22222222-2222-2222-2222-222222222227', '11111111-1111-1111-1111-111111111105', 'Peigne démêlant à dents larges', 'peigne-demelant', 'Démêle sans casser', 'Peigne en matière souple à dents larges, adapté aux cheveux crépus, bouclés et aux perruques.', 'LC-ACC-005', 1500, null, 60, 10, true, false),

    -- Entretien capillaire
    ('22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111106', 'Après-shampoing démêlant', 'apres-shampoing-demelant', 'Cheveux doux et faciles à coiffer', 'Après-shampoing au beurre de mangue et à l''aloe vera. Démêle et hydrate sans alourdir. Flacon 400 ml.', 'LC-ENT-003', 5000, null, 20, 5, true, false),
    ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111106', 'Masque capillaire à l''avocat', 'masque-capillaire-avocat', 'Nutrition intense en 10 minutes', 'Masque riche à l''huile d''avocat et au karité pour cheveux secs et abîmés. Laisser poser 10 minutes avant de rincer. Pot 300 ml.', 'LC-ENT-004', 6500, null, 12, 4, true, true),
    ('22222222-2222-2222-2222-222222222230', '11111111-1111-1111-1111-111111111106', 'Spray brillance (produit masqué)', 'spray-brillance', 'Produit inactif pour les tests', 'Produit volontairement désactivé : il ne doit pas apparaître dans la boutique, seulement dans /admin/produits.', 'LC-ENT-005', 4000, null, 10, 3, false, false)
on conflict (id) do update set
    category_id = excluded.category_id,
    name = excluded.name,
    slug = excluded.slug,
    short_description = excluded.short_description,
    description = excluded.description,
    sku = excluded.sku,
    price = excluded.price,
    compare_at_price = excluded.compare_at_price,
    stock_quantity = excluded.stock_quantity,
    low_stock_threshold = excluded.low_stock_threshold,
    active = excluded.active,
    featured = excluded.featured;
