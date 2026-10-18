-- Governance & Lineage Service: initial schema. Owns its own database, per the
-- platform's "no shared DB" rule.

create table audit_log_entry (
    id                uuid primary key,
    source_event_id   uuid         not null unique,
    source_service    varchar(128) not null,
    event_type        varchar(128) not null,
    actor_principal_type varchar(32),
    actor_principal_id   varchar(64),
    resource_path     varchar(1024),
    details_json      jsonb        not null,
    occurred_at       timestamptz  not null,
    ingested_at       timestamptz  not null default now()
);
-- Compliance queries are almost always "what happened to this resource" or
-- "what did this actor do", both over a time range.
create index idx_audit_resource_time on audit_log_entry (resource_path, occurred_at);
create index idx_audit_actor_time on audit_log_entry (actor_principal_type, actor_principal_id, occurred_at);

create table masking_policy (
    id           uuid primary key,
    name         varchar(255) not null,
    scope_path   varchar(1024) not null,
    rule_type    varchar(32)  not null,
    column_name  varchar(255),
    expression   text         not null,
    enabled      boolean      not null default true,
    created_at   timestamptz  not null default now(),
    created_by_user_id uuid
);
create index idx_masking_policy_scope on masking_policy (scope_path);

create table masking_policy_exempt_role (
    masking_policy_id uuid not null references masking_policy (id) on delete cascade,
    role_id           uuid not null
);

create table lineage_edge (
    id                  uuid primary key,
    source_path         varchar(1024) not null,
    source_column       varchar(255),
    target_path         varchar(1024) not null,
    target_column       varchar(255),
    transformation_type varchar(64),
    source_job_id       varchar(255),
    observed_at         timestamptz  not null default now()
);
-- Upstream traversal starts from target_path, downstream traversal starts from
-- source_path — both directions are queried often (impact analysis vs provenance).
create index idx_lineage_source on lineage_edge (source_path, source_column);
create index idx_lineage_target on lineage_edge (target_path, target_column);
