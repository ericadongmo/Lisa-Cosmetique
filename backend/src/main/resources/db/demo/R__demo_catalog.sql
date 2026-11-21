-- Données de démonstration (profil dev uniquement).
-- Migration répétable : ré-exécutée quand son contenu change, sans doublon.
insert into categories (id, name, slug, description, position, active) values
    ('11111111-1111-1111-1111-111111111101', 'Produits cosmétiques', 'produits-cosmetiques', 'Visage, maquillage, soins ciblés', 1, true),
    ('11111111-1111-1111-1111-111111111102', 'Laits et soins corporels', 'laits-soins-corporels', 'Hydratation et éclat au quotidien', 2, true),
    ('11111111-1111-1111-1111-111111111103', 'Perruques', 'perruques', 'Naturelles, lisses, bouclées', 3, true),
    ('11111111-1111-1111-1111-111111111104', 'Mèches et extensions', 'meches-extensions', 'Volume et longueur', 4, true),
    ('11111111-1111-1111-1111-111111111105', 'Accessoires de beauté', 'accessoires-beaute', 'Pinceaux, outils, petits essentiels', 5, true),
    ('11111111-1111-1111-1111-111111111106', 'Entretien capillaire', 'entretien-capillaire', 'Shampoings, huiles, soins', 6, true)
on conflict (id) do update set
    name = excluded.name,
    slug = excluded.slug,
    description = excluded.description,
    position = excluded.position,
    active = excluded.active;

insert into products (id, category_id, name, slug, short_description, description, sku, price, compare_at_price, stock_quantity, low_stock_threshold, active, featured) values
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111101', 'Sérum éclat à la vitamine C', 'serum-eclat-vitamine-c', 'Un teint lumineux dès quatre semaines', 'Sérum concentré à 10 % de vitamine C stabilisée, enrichi en acide hyaluronique. À appliquer matin et soir sur peau propre, avant la crème hydratante. Flacon 30 ml.', 'LC-COS-001', 12500, 15000, 18, 3, true, true),
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111101', 'Crème hydratante au karité', 'creme-hydratante-karite', 'Nourrit sans effet gras', 'Crème visage au beurre de karité et à la glycérine végétale, adaptée aux peaux sèches et mixtes. Texture fondante, sans parfum ajouté. Pot 50 ml.', 'LC-COS-002', 8000, null, 24, 5, true, true),
    ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111101', 'Fond de teint fluide', 'fond-de-teint-fluide', 'Couvrance modulable, fini naturel', 'Fond de teint léger à base d''huile de jojoba, disponible en plusieurs nuances. Tenue longue durée, sans effet masque. Flacon 30 ml.', 'LC-COS-003', 9500, null, 12, 3, true, false),
    ('22222222-2222-2222-2222-222222222204', '11111111-1111-1111-1111-111111111102', 'Beurre de karité brut', 'beurre-karite-brut', '100 % pur, non raffiné', 'Beurre de karité brut issu de coopératives camerounaises. Nourrit la peau et les cheveux, idéal en soin de nuit. Pot 250 g.', 'LC-COR-001', 5000, null, 30, 5, true, false),
    ('22222222-2222-2222-2222-222222222205', '11111111-1111-1111-1111-111111111102', 'Gommage sucre & vanille', 'gommage-sucre-vanille', 'Exfolie en douceur, parfum gourmand', 'Gommage corporel à base de sucre de canne et de vanille. À utiliser une à deux fois par semaine sur peau humide. Pot 200 g.', 'LC-COR-002', 6500, null, 16, 4, true, false),
    ('22222222-2222-2222-2222-222222222206', '11111111-1111-1111-1111-111111111102', 'Lait corporel nourrissant', 'lait-corporel-nourrissant', 'Hydratation 24 h', 'Lait corporel fluide au karité et à l''aloe vera, à absorption rapide. Convient à tous les types de peau. Flacon pompe 400 ml.', 'LC-COR-003', 7000, null, 20, 4, true, true),
    ('22222222-2222-2222-2222-222222222207', '11111111-1111-1111-1111-111111111103', 'Perruque lisse 100 % naturelle', 'perruque-lisse-naturelle', 'Cheveux naturels, lace frontale 13x4', 'Perruque en cheveux naturels lissés, lace frontale 13x4, densité 180 %. Longueur 50 cm, ajustable par bandes élastiques.', 'LC-PER-001', 85000, 95000, 4, 2, true, true),
    ('22222222-2222-2222-2222-222222222208', '11111111-1111-1111-1111-111111111103', 'Perruque bouclée', 'perruque-bouclee', 'Boucles souples, effet volume', 'Perruque en cheveux naturels bouclés, densité 150 %, longueur 45 cm. Se coiffe au doigt ou avec un leave-in léger.', 'LC-PER-002', 72000, null, 3, 2, true, false),
    ('22222222-2222-2222-2222-222222222209', '11111111-1111-1111-1111-111111111104', 'Mèches naturelles — 3 bundles', 'meches-naturelles-3-bundles', 'Lot de 3 mèches en cheveux naturels', 'Lot de trois bundles de cheveux naturels lisses, longueur 55 cm, couleur naturelle (1B). Idéal pour une pose complète.', 'LC-MEC-001', 65000, null, 6, 2, true, true),
    ('22222222-2222-2222-2222-222222222210', '11111111-1111-1111-1111-111111111104', 'Closure frontal 13x4', 'closure-frontal-13x4', 'Finition naturelle sur le devant', 'Closure frontal en cheveux naturels, 13 pouces par 4, longueur 40 cm. Se pose avec une colle de qualité professionnelle.', 'LC-MEC-002', 45000, null, 5, 2, true, false),
    ('22222222-2222-2222-2222-222222222211', '11111111-1111-1111-1111-111111111105', 'Set de 12 pinceaux', 'set-12-pinceaux', 'Visage et yeux, poils synthétiques', 'Jeu de douze pinceaux en poils synthétiques doux, avec pochette de rangement. Se nettoie à l''eau tiède et au savon doux.', 'LC-ACC-001', 15000, null, 14, 3, true, false),
    ('22222222-2222-2222-2222-222222222212', '11111111-1111-1111-1111-111111111105', 'Éponge de maquillage', 'eponge-maquillage', 'Fini lisse, réutilisable', 'Éponge en mousse sans latex pour fond de teint et correcteur. Se nettoie après chaque utilisation.', 'LC-ACC-002', 2500, null, 40, 8, true, false),
    ('22222222-2222-2222-2222-222222222213', '11111111-1111-1111-1111-111111111106', 'Shampoing hydratant', 'shampoing-hydratant', 'Formule douce sans sulfates', 'Shampoing sans sulfates à l''huile de coco, adapté aux cheveux naturels et aux perruques. Flacon 500 ml.', 'LC-ENT-001', 4500, null, 25, 5, true, false),
    ('22222222-2222-2222-2222-222222222214', '11111111-1111-1111-1111-111111111106', 'Huile de ricin pure', 'huile-ricin-pure', 'Renforce cheveux et cils', 'Huile de ricin pressée à froid, non diluée. À appliquer en massage sur le cuir chevelu ou sur les cils. Flacon 100 ml.', 'LC-ENT-002', 3500, null, 32, 6, true, true)
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

insert into delivery_zones (id, name, city, delivery_fee, estimated_delivery_days, active) values
    ('33333333-3333-3333-3333-333333333301', 'Yaoundé centre', 'Yaoundé', 1500, 1, true),
    ('33333333-3333-3333-3333-333333333302', 'Douala', 'Douala', 3000, 2, true),
    ('33333333-3333-3333-3333-333333333303', 'Autres villes', 'Cameroun', 4000, 3, true)
on conflict (id) do update set
    name = excluded.name,
    city = excluded.city,
    delivery_fee = excluded.delivery_fee,
    estimated_delivery_days = excluded.estimated_delivery_days,
    active = excluded.active;
