-- Utilisateurs et rôles. Les mots de passe sont toujours hachés côté application.
create table users (
    id             uuid         primary key,
    first_name     varchar(80)  not null,
    last_name      varchar(80)  not null,
    email          varchar(320) not null,
    phone_number   varchar(30),
    password_hash  varchar(255) not null,
    role           varchar(20)  not null,
    enabled        boolean      not null default true,
    email_verified boolean      not null default false,
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now(),

    constraint uk_users_email unique (email),
    constraint uk_users_phone unique (phone_number),
    constraint ck_users_role check (role in ('ADMIN', 'CUSTOMER'))
);

-- Emails stockés en minuscules par l'application ; ce CHECK évite les doublons de casse.
alter table users add constraint ck_users_email_lowercase check (email = lower(email));

-- Supabase expose le schéma public en REST : on bloque l'accès direct.
alter table users enable row level security;
