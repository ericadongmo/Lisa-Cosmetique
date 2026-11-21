-- Catalogue : catégories, produits et métadonnées d'images.
-- Les fichiers vivent dans Supabase Storage ; la base ne garde que les clés.

create table categories (
    id          uuid         primary key,
    name        varchar(120) not null,
    slug        varchar(80)  not null,
    description varchar(500),
    image_key   varchar(512),
    active      boolean      not null default true,
    position    integer      not null default 0,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    created_by  uuid         references users (id) on delete set null,
    updated_by  uuid         references users (id) on delete set null,

    constraint uk_categories_slug unique (slug),
    constraint ck_categories_position check (position >= 0)
);

create table products (
    id                  uuid         primary key,
    category_id         uuid         not null references categories (id) on delete restrict,
    name                varchar(160) not null,
    slug                varchar(120) not null,
    short_description   varchar(300),
    description         text,
    sku                 varchar(64)  not null,
    price               bigint       not null,
    compare_at_price    bigint,
    stock_quantity      integer      not null default 0,
    low_stock_threshold integer      not null default 3,
    active              boolean      not null default true,
    featured            boolean      not null default false,
    version             bigint       not null default 0,
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now(),
    created_by          uuid         references users (id) on delete set null,
    updated_by          uuid         references users (id) on delete set null,

    constraint uk_products_slug unique (slug),
    constraint uk_products_sku unique (sku),
    constraint ck_products_price check (price >= 0),
    constraint ck_products_compare_at_price check (compare_at_price is null or compare_at_price > price),
    constraint ck_products_stock check (stock_quantity >= 0),
    constraint ck_products_low_stock check (low_stock_threshold >= 0)
);

create index idx_products_category on products (category_id);
create index idx_products_active_featured on products (active, featured);

create table product_images (
    id         uuid         primary key,
    product_id uuid         not null references products (id) on delete cascade,
    object_key varchar(512) not null,
    alt_text   varchar(200),
    position   integer      not null default 0,
    is_primary boolean      not null default false,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now(),

    constraint uk_product_images_object_key unique (object_key),
    constraint uk_product_images_position unique (product_id, position),
    constraint ck_product_images_position check (position >= 0)
);

-- Une seule image principale par produit.
create unique index uk_product_images_primary
    on product_images (product_id) where is_primary;

alter table categories enable row level security;
alter table products enable row level security;
alter table product_images enable row level security;
