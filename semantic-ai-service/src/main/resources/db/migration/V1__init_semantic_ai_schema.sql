-- Semantic AI Service: initial schema. Owns its own database, per the platform's
-- "no shared DB" rule.

-- Requires the pgvector extension. Most managed Postgres offerings (RDS, Cloud SQL,
-- Aiven, etc.) support `CREATE EXTENSION vector` directly; self-hosted Postgres
-- needs the pgvector extension package installed on the server first (the
-- `pgvector/pgvector` Docker image, or `apt install postgresql-16-pgvector`, etc.).
-- Quarkus Dev Services' default Postgres image does NOT include pgvector — see
-- module README "Running locally" for the dev-services image override needed to
-- run this migration successfully in `quarkus:dev`/tests.
create extension if not exists vector;

create table dataset_embedding (
    id               uuid primary key,
    dataset_path     varchar(1024) not null,
    column_name      varchar(255),
    target_type      varchar(16)   not null,
    source_text      text          not null,
    embedding_model  varchar(128)  not null,
    -- Dimension pinned to 1536 (OpenAI text-embedding-3-small / Voyage-2 default
    -- dimension family). Changing embedding model to one with a different dimension
    -- requires a new migration altering this column — mixed dimensions in one table
    -- cannot be searched against each other meaningfully anyway, so a model change
    -- is a "re-embed everything" operation regardless.
    embedding        vector(1536)  not null,
    created_at       timestamptz   not null default now(),
    updated_at       timestamptz   not null default now(),
    unique (dataset_path, column_name, target_type)
);
-- IVFFlat approximate-nearest-neighbor index for cosine distance — good default for
-- moderate-scale catalogs (thousands to low millions of embedded objects). At much
-- larger scale, consider HNSW (pgvector >= 0.5.0, better recall/latency tradeoff at
-- the cost of slower index builds) — noted here rather than switched to preemptively,
-- since HNSW build time matters more once there's real data volume to build against.
create index idx_dataset_embedding_vector on dataset_embedding
    using ivfflat (embedding vector_cosine_ops) with (lists = 100);
create index idx_dataset_embedding_path on dataset_embedding (dataset_path);

create table wiki_suggestion (
    id                     uuid primary key,
    dataset_path           varchar(1024) not null,
    column_name            varchar(255),
    suggested_description  text          not null,
    status                 varchar(16)   not null default 'PENDING',
    generated_by_model     varchar(128)  not null,
    created_at             timestamptz   not null default now(),
    reviewed_by_user_id    uuid,
    reviewed_at            timestamptz,
    review_note            text
);
create index idx_wiki_suggestion_status on wiki_suggestion (status, created_at);
create index idx_wiki_suggestion_path on wiki_suggestion (dataset_path);

create table ai_function_invocation (
    id                   uuid primary key,
    function_name        varchar(64)  not null,
    input_text           text         not null,
    output_text          text,
    model                varchar(128) not null,
    prompt_tokens        int,
    completion_tokens    int,
    principal_type       varchar(32),
    principal_id         varchar(64),
    source_dataset_path  varchar(1024),
    invoked_at           timestamptz  not null default now(),
    latency_millis       bigint,
    failed               boolean      not null default false,
    error_message        text
);
-- Cost/usage-tracking queries are almost always "by principal, over a time range".
create index idx_ai_invocation_principal_time on ai_function_invocation (principal_type, principal_id, invoked_at desc);
