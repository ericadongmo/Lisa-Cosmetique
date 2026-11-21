-- Zones de livraison configurables depuis l'administration.
create table delivery_zones (
    id                      uuid         primary key,
    name                    varchar(120) not null,
    city                    varchar(120) not null,
    delivery_fee            bigint       not null,
    estimated_delivery_days integer      not null,
    active                  boolean      not null default true,
    created_at              timestamptz  not null default now(),
    updated_at              timestamptz  not null default now(),
    created_by              uuid         references users (id) on delete set null,
    updated_by              uuid         references users (id) on delete set null,

    constraint uk_delivery_zones_city_name unique (city, name),
    constraint ck_delivery_zones_fee check (delivery_fee >= 0),
    constraint ck_delivery_zones_days check (estimated_delivery_days >= 0)
);

alter table delivery_zones enable row level security;
