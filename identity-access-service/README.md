# Identity & Access Service

Phase 1 of the roadmap in `dremio-reactive-extension-plan.md`. Owns local + federated
identities, RBAC (custom roles, scoped role assignments), personal access tokens, and
service accounts. Issues short-lived JWTs the `dremio-adapter` validates inline on the
Dremio coordinator.

## Status

| # | Layer | Status |
|---|---|---|
| 1 | Domain model (`domain/`) | ✅ done |
| 2 | Repository layer (`repository/`) | ✅ done |
| 3 | Service layer (`service/`, `service/dto/`, `event/`) | ✅ done |
| 4 | REST layer (`resource/`) | ✅ done |
| 5 | gRPC `AuthCheckService` (`grpc/`, `proto/auth_check.proto`) | ✅ done (JWT branch of `ResolvePrincipal` stubbed — see note below) |
| 6 | Tests (`src/test/`) | 🟡 started — REST-layer integration tests for users, roles, and scope-cascading authz; unit tests and gRPC-layer tests still to add |

## Domain model

```
User ───────────┐
                 ├── RoleAssignment (principalType, principalId, role, scopeType, scopePath)
ServiceAccount ──┘

Role ── RolePermission ── Permission

User / ServiceAccount ── PersonalAccessToken

IdentityProviderConfig  (standalone; referenced by User.identityProviderId)
```

## Layers built so far

- **Repository** (`repository/`) — Panache reactive repositories. The one worth
  reading closely is `RoleAssignmentRepository.findApplicableRoleIds`: a single JPQL
  query resolves every role granted to a principal at a scope path *or any ancestor
  of it*, using a `LIKE concat(scopePath, '.%')` segment-boundary match. Grants
  cascade down the hierarchy (additive, no explicit-deny) — narrowing what a broad
  grant can actually see is the Governance Service's masking-policy job, not this
  service's. `RolePermissionRepository` only ever returns flat projections (never a
  lazy entity collection) since Hibernate Reactive sessions can't survive touching
  lazy state outside their original scope.
- **Service** (`service/`) — `UserService`, `RoleService`, `RoleAssignmentService`,
  `PersonalAccessTokenService`, `ServiceAccountService`. Every mutating method
  publishes a domain event (`event/`) to the `identity.events` Kafka topic after its
  write — consumed by the Governance Service for audit ingestion and by the API
  Gateway to invalidate short-TTL authz caches. PATs: only a SHA-256 hash + 8-char
  display prefix are ever persisted; the raw token is returned once, in
  `CreatePatResponse`, and never logged.
- **REST** (`resource/`) — JAX-RS reactive resources per aggregate, plus
  `PlatformExceptionMapper` translating every `PlatformException` into the shared
  RFC 7807 body from `platform-common`. **Note:** actor identity is read from an
  `X-Actor-Id` header in this scaffold as a placeholder — real request-scoped
  identity (from the validated JWT/PAT) is wired in as part of the security-context
  work, not before the REST layer ships. Don't treat this header as authenticated.
- **gRPC** (`grpc/`, `proto/auth_check.proto`) — `AuthCheckService`: the inline
  call the `dremio-adapter`'s `AccessControlChecker` implementation makes on every
  coordinator request. `CheckPermission`/`CheckPermissions` fail closed (return
  `allowed = false`) on any resolution error, per the plan's stated mitigation for
  policy-service unavailability. `ResolvePrincipal`'s PAT branch is fully wired; its
  JWT branch is a fail-closed stub pending JWT issuance (increment: JWT signing key
  management + `smallrye-jwt-build` wiring — not yet built).

## Running locally

```bash
mvn -pl identity-access-service -am quarkus:dev
```

Quarkus Dev Services spins up throwaway Postgres and Kafka containers via
Testcontainers; Flyway applies `V1__init_identity_schema.sql` automatically. No
manual DB/Kafka setup needed for local development or for running the test suite.

```bash
mvn -pl identity-access-service -am test
```

## Known gaps / next continuation prompts

- **JWT issuance + verification** — `smallrye-jwt-build` is on the classpath and
  `application.properties` has issuer/lifespan configured, but no service method
  actually mints a token yet, and `ResolvePrincipal`'s JWT branch is a stub.
- **SCIM inbound provisioning** — `IdentityProviderConfig.scimEndpoint` exists on
  the entity but nothing consumes it yet.
- **OIDC/LDAP login flows** — `quarkus-oidc` and `quarkus-elytron-security-ldap`
  are dependencies; the actual federation handshake / callback resource isn't built.
- **gRPC and unit test coverage** — current tests are REST-layer integration tests
  only (via `@QuarkusTest` + REST-assured against Dev Services Postgres/Kafka).
  `AuthCheckServiceImpl` has no direct test yet.

Suggested next continuation prompt: **"Wire up JWT issuance in identity-access-service"**
— a `TokenService` that mints signed JWTs on successful local/federated login, a
`/api/v1/auth/login` endpoint, and completing `ResolvePrincipal`'s JWT branch to
verify them.
