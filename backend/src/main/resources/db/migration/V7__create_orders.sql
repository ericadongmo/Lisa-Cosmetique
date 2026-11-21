-- Commandes : montants toujours recalculés côté backend.
-- Une commande conserve un instantané de l'adresse et de chaque produit acheté.
create table orders (
    id                       uuid         primary key,
    order_number             varchar(32)  not null,
    user_id                  uuid         references users (id) on delete set null,
    customer_name            varchar(160) not null,
    customer_phone           varchar(30)  not null,
    customer_email           varchar(320),
    delivery_address_snapshot jsonb       not null,
    delivery_zone_id         uuid         not null references delivery_zones (id) on delete restrict,
    subtotal                 bigint       not null,
    delivery_fee             bigint       not null,
    discount_amount          bigint       not null default 0,
    total_amount             bigint       not null,
    order_status             varchar(20)  not null,
    payment_status           varchar(20)  not null,
    payment_method           varchar(30)  not null,
    notes                    varchar(1000),
    promotion_id             uuid         references promotions (id) on delete set null,
    version                  bigint       not null default 0,
    created_at               timestamptz  not null default now(),
    updated_at               timestamptz  not null default now(),

    constraint uk_orders_number unique (order_number),
    constraint ck_orders_status check (order_status in
        ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    constraint ck_orders_payment_status check (payment_status in
        ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED')),
    constraint ck_orders_payment_method check (payment_method in
        ('MTN_MOMO', 'ORANGE_MONEY', 'CASH_ON_DELIVERY')),
    constraint ck_orders_amounts check (
        subtotal >= 0 and delivery_fee >= 0 and discount_amount >= 0 and total_amount >= 0),
    constraint ck_orders_total check (
        total_amount = subtotal + delivery_fee - discount_amount)
);

create index idx_orders_user on orders (user_id, created_at desc);
create index idx_orders_delivery_zone on orders (delivery_zone_id);
create index idx_orders_status on orders (order_status);
create index idx_orders_created_at on orders (created_at desc);

create table order_items (
    id                    uuid         primary key,
    order_id              uuid         not null references orders (id) on delete cascade,
    product_id            uuid         references products (id) on delete set null,
    product_name_snapshot varchar(160) not null,
    product_sku_snapshot  varchar(64)  not null,
    unit_price            bigint       not null,
    quantity              integer      not null,
    subtotal              bigint       not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now(),

    constraint ck_order_items_unit_price check (unit_price >= 0),
    constraint ck_order_items_quantity check (quantity > 0),
    constraint ck_order_items_subtotal check (subtotal >= 0 and subtotal = unit_price * quantity)
);

create index idx_order_items_order on order_items (order_id);
create index idx_order_items_product on order_items (product_id);

alter table orders enable row level security;
alter table order_items enable row level security;
