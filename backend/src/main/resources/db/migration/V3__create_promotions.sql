-- Promotions : remise en pourcentage ou montant fixe, sur une sélection de produits.
-- Règles : pas de cumul, une seule promotion appliquée par commande (la plus avantageuse).
create table promotions (
    id             uuid          primary key,
    name           varchar(120)  not null,
    description    varchar(500),
    discount_type  varchar(20)   not null,
    discount_value numeric(12, 2) not null,
    start_date     timestamptz   not null,
    end_date       timestamptz   not null,
    active         boolean       not null default true,
    created_at     timestamptz   not null default now(),
    updated_at     timestamptz   not null default now(),
    created_by     uuid          references users (id) on delete set null,
    updated_by     uuid          references users (id) on delete set null,

    constraint ck_promotions_type check (discount_type in ('PERCENTAGE', 'FIXED_AMOUNT')),
    constraint ck_promotions_value check (discount_value > 0),
    constraint ck_promotions_percentage check (
        discount_type <> 'PERCENTAGE' or discount_value <= 100),
    constraint ck_promotions_dates check (end_date > start_date)
);

create table promotion_products (
    promotion_id uuid not null references promotions (id) on delete cascade,
    product_id   uuid not null references products (id) on delete cascade,

    primary key (promotion_id, product_id)
);

create index idx_promotion_products_product on promotion_products (product_id);
create index idx_promotions_window on promotions (active, start_date, end_date);

alter table promotions enable row level security;
alter table promotion_products enable row level security;
