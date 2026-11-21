-- Suivi de livraison. Un seul envoi par commande dans cette version ;
-- la contrainte unique pourra être retirée si les expéditions partielles arrivent.
create table shipments (
    id               uuid        primary key,
    order_id         uuid        not null references orders (id) on delete cascade,
    delivery_zone_id uuid        not null references delivery_zones (id) on delete restrict,
    tracking_number  varchar(64),
    shipment_status  varchar(20) not null,
    shipped_at       timestamptz,
    delivered_at     timestamptz,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),

    constraint uk_shipments_order unique (order_id),
    constraint uk_shipments_tracking unique (tracking_number),
    constraint ck_shipments_status check (shipment_status in
        ('PENDING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    constraint ck_shipments_dates check (
        delivered_at is null or (shipped_at is not null and delivered_at >= shipped_at))
);

create index idx_shipments_zone on shipments (delivery_zone_id);

alter table shipments enable row level security;
