-- Identity & Access Service: initial schema.
-- Owns its own database/schema per the platform's "no shared DB" rule
-- (see dremio-reactive-extension-plan.md, Section 6).

create table app_user (
    id                  uuid primary key,
    username            varchar(255) not null unique,
    email               varchar(320) not null unique,
    display_name        varchar(255),
    identity_provider_id uuid,
    external_subject    varchar(512),
    local_password_hash varchar(255),
    status              varchar(32)  not null default 'ACTIVE',
    created_at          timestamptz  not null default now(),
    last_login_at       timestamptz,
    version             bigint       not null default 0
);
create index idx_app_user_external_subject on app_user (identity_provider_id, external_subject);

create table service_account (
    id            uuid primary key,
    name          varchar(255) not null unique,
    description   text,
    owner_user_id uuid references app_user (id),
    enabled       boolean      not null default true,
    created_at    timestamptz  not null default now()
);

create table role (
    id          uuid primary key,
    name        varchar(255) not null unique,
    description text,
    system_role boolean      not null default false,
    created_at  timestamptz  not null default now()
);

create table permission (
    id          uuid primary key,
    key         varchar(255) not null unique,
    domain      varchar(64)  not null,
    description text
);

create table role_permission (
    id            uuid primary key,
    role_id       uuid not null references role (id) on delete cascade,
    permission_id uuid not null references permission (id) on delete cascade,
    unique (role_id, permission_id)
);

create table role_assignment (
    id               uuid primary key,
    principal_type   varchar(32)  not null,
    principal_id     uuid         not null,
    role_id          uuid         not null references role (id) on delete cascade,
    scope_type       varchar(32)  not null,
    scope_path       varchar(1024) not null,
    granted_by_user_id uuid,
    granted_at       timestamptz  not null default now(),
    expires_at       timestamptz
);
-- Every authz check is "find assignments for this principal whose scope_path
-- is a prefix of the requested path" — this index makes that a range scan.
create index idx_role_assignment_principal_scope on role_assignment (principal_type, principal_id, scope_path);

create table personal_access_token (
    id             uuid primary key,
    principal_type varchar(32)  not null,
    principal_id   uuid         not null,
    label          varchar(255) not null,
    token_hash     varchar(128) not null unique,
    display_prefix varchar(16)  not null,
    status         varchar(32)  not null default 'ACTIVE',
    created_at     timestamptz  not null default now(),
    expires_at     timestamptz,
    last_used_at   timestamptz,
    revoked_at     timestamptz
);
create index idx_pat_principal on personal_access_token (principal_type, principal_id);

create table identity_provider_config (
    id                     uuid primary key,
    name                   varchar(255) not null unique,
    type                   varchar(32)  not null,
    connection_config_json jsonb        not null,
    enabled                boolean      not null default true,
    scim_endpoint          varchar(1024),
    created_at             timestamptz  not null default now()
);

-- ---------------------------------------------------------------------------
-- Seed data: baseline permission catalog + system roles.
-- Additional permission keys are added by later services' own migrations
-- registering into this shared reference table (see module README).
-- ---------------------------------------------------------------------------

insert into permission (id, key, domain, description) values
    (gen_random_uuid(), 'identity.user.manage',            'identity',   'Create, suspend, deprovision users'),
    (gen_random_uuid(), 'identity.role.manage',             'identity',   'Create/edit custom roles and permissions'),
    (gen_random_uuid(), 'identity.role-assignment.manage',  'identity',   'Grant/revoke role assignments'),
    (gen_random_uuid(), 'identity.pat.manage-own',          'identity',   'Create/revoke own personal access tokens'),
    (gen_random_uuid(), 'identity.idp.manage',              'identity',   'Configure external identity providers'),
    (gen_random_uuid(), 'catalog.dataset.read',             'catalog',    'Read data from a dataset'),
    (gen_random_uuid(), 'catalog.dataset.write',            'catalog',    'Write/modify a dataset'),
    (gen_random_uuid(), 'catalog.namespace.manage',         'catalog',    'Create/rename/delete spaces, folders'),
    (gen_random_uuid(), 'governance.masking-policy.manage', 'governance', 'Create/edit row/column masking policies'),
    (gen_random_uuid(), 'governance.audit.read',            'governance', 'Query audit history'),
    (gen_random_uuid(), 'workload.engine.scale',            'workload',   'Start/stop/scale elastic engines');

insert into role (id, name, description, system_role)
values (gen_random_uuid(), 'ORG_ADMIN', 'Full administrative access to the organization', true);

-- Grant every seeded permission to ORG_ADMIN.
insert into role_permission (id, role_id, permission_id)
select gen_random_uuid(), r.id, p.id
from role r, permission p
where r.name = 'ORG_ADMIN';

insert into role (id, name, description, system_role)
values (gen_random_uuid(), 'DATA_READER', 'Read-only access to datasets in scope', true);

insert into role_permission (id, role_id, permission_id)
select gen_random_uuid(), r.id, p.id
from role r, permission p
where r.name = 'DATA_READER' and p.key = 'catalog.dataset.read';
