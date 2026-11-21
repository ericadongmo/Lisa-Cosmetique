-- Paiements : plusieurs tentatives possibles par commande.
-- Un paiement n'est SUCCESS qu'après vérification serveur du prestataire.
create table payments (
    id                      uuid         primary key,
    order_id                uuid         not null references orders (id) on delete restrict,
    payment_reference       varchar(64)  not null,
    provider                varchar(40)  not null,
    payment_method          varchar(30)  not null,
    amount                  bigint       not null,
    currency                varchar(3)  not null default 'XAF',
    payment_status          varchar(20)  not null,
    provider_transaction_id varchar(128),
    paid_at                 timestamptz,
    created_at              timestamptz  not null default now(),
    updated_at              timestamptz  not null default now(),

    constraint uk_payments_reference unique (payment_reference),
    constraint ck_payments_method check (payment_method in
        ('MTN_MOMO', 'ORANGE_MONEY', 'CASH_ON_DELIVERY')),
    constraint ck_payments_status check (payment_status in
        ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED')),
    constraint ck_payments_amount check (amount > 0),
    constraint ck_payments_paid_at check (payment_status <> 'SUCCESS' or paid_at is not null)
);

create unique index uk_payments_provider_transaction
    on payments (provider, provider_transaction_id)
    where provider_transaction_id is not null;

create index idx_payments_order on payments (order_id);

-- Journal brut des notifications : la contrainte d'unicité garantit l'idempotence.
create table payment_events (
    id                uuid        primary key,
    payment_id        uuid        not null references payments (id) on delete cascade,
    provider          varchar(40) not null,
    provider_event_id varchar(128) not null,
    payload           jsonb       not null,
    received_at       timestamptz not null default now(),
    processed_at      timestamptz,

    constraint uk_payment_events_provider_event unique (provider, provider_event_id)
);

create index idx_payment_events_payment on payment_events (payment_id);

alter table payments enable row level security;
alter table payment_events enable row level security;
