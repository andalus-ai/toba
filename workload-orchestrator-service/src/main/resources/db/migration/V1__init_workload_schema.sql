-- Workload Orchestration Service: initial schema. Owns its own database, per the
-- platform's "no shared DB" rule.

create table engine (
    id                     uuid primary key,
    name                   varchar(255) not null unique,
    description            text,
    min_replicas           int          not null default 0,
    max_replicas           int          not null default 10,
    current_replicas       int          not null default 0,
    status                 varchar(32)  not null default 'STOPPED',
    auto_stop_enabled      boolean      not null default true,
    auto_stop_idle_minutes int          not null default 15,
    last_activity_at       timestamptz,
    k8s_namespace          varchar(255) not null,
    k8s_deployment_name    varchar(255) not null,
    created_at             timestamptz  not null default now(),
    version                bigint       not null default 0
);

-- The idle-auto-stop scheduled job's core query: every RUNNING, auto-stop-enabled
-- engine, cheapest to find via a partial index on status.
create index idx_engine_running_autostop on engine (status, auto_stop_enabled, last_activity_at)
    where status = 'RUNNING' and auto_stop_enabled = true;

create table wlm_queue_rule (
    id                     uuid primary key,
    name                   varchar(255) not null,
    engine_id              uuid         not null references engine (id) on delete cascade,
    priority               int          not null,
    query_tag_pattern      varchar(512) not null,
    max_concurrent_queries int          not null default 50,
    queue_timeout_seconds  int          not null default 300,
    enabled                boolean      not null default true,
    created_at             timestamptz  not null default now()
);
-- Rule resolution scans enabled rules in priority order — this index covers it directly.
create index idx_wlm_rule_priority on wlm_queue_rule (enabled, priority desc, created_at asc);

create table engine_scaling_event (
    id                 uuid primary key,
    engine_id          uuid         not null references engine (id) on delete cascade,
    event_type         varchar(32)  not null,
    from_replicas      int          not null,
    to_replicas        int          not null,
    reason             text,
    actor_principal_id uuid,
    occurred_at        timestamptz  not null default now()
);
create index idx_engine_scaling_event_engine_time on engine_scaling_event (engine_id, occurred_at desc);
