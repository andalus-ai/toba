-- Catalog Service: initial schema. Owns its own database, per the platform's
-- "no shared DB" rule.

create table catalog_namespace (
    id             uuid primary key,
    levels_path    varchar(1024) not null unique,
    properties_json jsonb        not null default '{}',
    created_at     timestamptz  not null default now()
);

create table catalog_table (
    id                 uuid primary key,
    namespace_path     varchar(1024) not null,
    table_name         varchar(255)  not null,
    metadata_location  varchar(2048) not null,
    properties_json    jsonb         not null default '{}',
    current_snapshot_id bigint,
    created_at         timestamptz   not null default now(),
    last_updated_at    timestamptz   not null default now(),
    unique (namespace_path, table_name)
);
create index idx_catalog_table_namespace on catalog_table (namespace_path);

create table table_snapshot (
    id                     uuid primary key,
    catalog_table_id       uuid not null references catalog_table (id) on delete cascade,
    snapshot_id            bigint not null,
    parent_snapshot_id     bigint,
    manifest_list_location varchar(2048) not null,
    summary_json           jsonb not null default '{}',
    committed_at           timestamptz not null default now(),
    unique (catalog_table_id, snapshot_id)
);
-- Time-travel and "list snapshots for a table, most recent first" are the two
-- access patterns; this index covers both.
create index idx_table_snapshot_table_time on table_snapshot (catalog_table_id, committed_at desc);
