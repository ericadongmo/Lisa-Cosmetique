-- Paniers : un par client connecté, ou un panier invité identifié par un jeton.
-- Le jeton brut n'est jamais stocké : seule son empreinte SHA-256 est en base.
create table carts (
    id                 uuid        primary key,
    user_id            uuid        references users (id) on delete cascade,
    session_token_hash varchar(64),
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),

    constraint uk_carts_session_token unique (session_token_hash),
    constraint ck_carts_owner check (user_id is not null or session_token_hash is not null)
);

create unique index uk_carts_user on carts (user_id) where user_id is not null;

create table cart_items (
    id         uuid        primary key,
    cart_id    uuid        not null references carts (id) on delete cascade,
    product_id uuid        not null references products (id) on delete restrict,
    quantity   integer     not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint uk_cart_items_product unique (cart_id, product_id),
    constraint ck_cart_items_quantity check (quantity > 0)
);

create index idx_cart_items_cart on cart_items (cart_id);

alter table carts enable row level security;
alter table cart_items enable row level security;
