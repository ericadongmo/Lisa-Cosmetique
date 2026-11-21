-- Adresses de livraison. Une seule adresse par défaut par client.
create table addresses (
    id                     uuid         primary key,
    user_id                uuid         not null references users (id) on delete cascade,
    recipient_name         varchar(160) not null,
    phone_number           varchar(30)  not null,
    city                   varchar(120) not null,
    neighborhood           varchar(160) not null,
    street_address         varchar(300) not null,
    additional_information varchar(500),
    is_default             boolean      not null default false,
    created_at             timestamptz  not null default now(),
    updated_at             timestamptz  not null default now()
);

create index idx_addresses_user on addresses (user_id);
create unique index uk_addresses_default on addresses (user_id) where is_default;

alter table addresses enable row level security;
