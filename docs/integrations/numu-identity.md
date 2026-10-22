# Numu interoperability — implementation contract

Toba's shared identity integration is **optional**. Toba remains deployable without Numu, and Toba retains responsibility for lakehouse authorization, engine grants, masking and query governance.

## Verified source modules

- `identity/numou-identity-domain`: user aggregate and identity value objects.
- `identity/numou-identity-application`: user lifecycle use cases and repository/hasher ports.
- `identity/numou-identity-adapter`: Quarkus REST, Hibernate Reactive, Kafka adapters.
- `core/framework/scope/scope-domain`, `scope-spi`, `scope-application`, `scope-postgres-adapter`: neutral breakdown structures; **not assumed to be a tenant authority**.

The source currently uses `tech.kayys.numou` coordinates and `0.1.0-SNAPSHOT`. Do not add a hard Maven dependency until packages, artifact publication and compatibility have been validated.

## Implemented boundary

`ExternalIdentityResolver` defines the minimum verified identity mapping: trusted subject and issuer -> immutable user UUID, tenant UUID, and active status. It is a provider contract only; **no Numu adapter or authentication wiring is implemented yet**. A future Numu adapter must validate tokens and issuer/audience before calling this contract; it must not treat a request header as proof of identity.

## Next integration gates

1. Identify Numu's canonical tenant-membership source; its neutral scope module is not automatically a tenant service.
2. Create a provider adapter and verified subject mapping, with cache invalidation on user disablement.
3. Wire the adapter behind an explicit configuration feature flag; deny by default if selected provider is unavailable.
4. Enforce membership in Toba's catalog and query authorization evidence.
5. Add contract tests for disabled users, wrong tenant, unknown issuer, expired credentials, and missing provider.

## Confirmed Numu membership API

Numu provides `tech.kayys.numou.identity.application.port.TenantMembershipPort`:
`Uni<Boolean> isMember(Principal principal, String tenantId)` and `Uni<Set<String>> roles(...)`.

Its `Principal` record requires a `UserId` for USER principals; `UserId.of(UUID)` is available. Numu's `TenantMembership` domain recognizes ACTIVE, SUSPENDED and REVOKED lifecycle states. The Numu adapter must confirm that `isMember` means *active* membership in its concrete implementation before Toba relies on it.

A proposed adapter should map Toba UUIDs to Numu `UserId.of(userId)` and `tenantId.toString()`, and pass a principal constructed from verified upstream identity context. Do **not** fabricate authenticated principals from arbitrary user IDs, and do not treat Numu's global roles as Toba data grants.

The adapter remains unimplemented pending verified Maven artifact resolution and a trusted principal mapping. `VerifiedIdentityBridge.requireActiveMembership` already requires an injected `TenantMembershipVerifier` and denies access if none is available.

## Optional adapter implementation

The opt-in adapter now lives at `identity-access-service/src/numu/java/.../NumuTenantMembershipVerifier.java`. It is excluded from the default source tree and added only by the Maven profile `numu-identity-integration`, activated with `-Dtoba.numu.identity=true`. This keeps Toba buildable without Numu artifacts.

Before enabling the profile, build/install Numu's identity application and required reactor modules into the local Maven repository. Then build Toba with:

```sh
mvn -pl identity-access-service -am test -Dtoba.numu.identity=true
```

The adapter maps canonical UUIDs to Numu's `UserId` and tenant string and delegates membership to `TenantMembershipPort`; provider failures return denial. It must only be called with canonical identity values returned by verified authentication and identity resolution. It does not authenticate a user.

**Integration gate:** inspect and test the concrete Numu `TenantMembershipPort` implementation to confirm `isMember` returns true only for ACTIVE memberships. If it includes invited or suspended memberships, this adapter must be adjusted to use a status-aware query before production.

## SPI correction: use Numu's authoritative active-membership resolver

Repository inspection found the concrete Numu implementation:
- `IdentityTenantMembershipAdapter` implements `TenantMembershipResolver`.
- It delegates to `MembershipManagementPort.isActive(tenantId, userId)`.
- The PostgreSQL implementation checks both membership status `ACTIVE` and user status `ACTIVE`.

The optional Toba adapter now uses `numou-security-spi` and `TenantMembershipResolver`, rather than calling the identity application `TenantMembershipPort` directly. This follows Numu's security boundary and active-membership semantics.

The adapter builds Numu's principal value from the canonical UUID returned by Toba's verified identity mapping. This is identity translation, **not authentication**: callers must never pass a user ID taken directly from an untrusted request. Toba still validates the resolved tenant and requires this membership verifier before tenant-scoped access.

Enable with `-Dtoba.numu.identity=true` only after installing compatible Numu reactor artifacts. Build and integration tests remain to be run in an environment with both repositories and dependencies available.

## Membership adapter contract tests

The opt-in profile also adds `src/numu-test/java` as a test source directory. The adapter contract suite checks that:
- valid canonical IDs are delegated to the Numu `TenantMembershipResolver`;
- inactive/non-member results are denied;
- resolver failures fail closed as `false`;
- null user or tenant identifiers are denied without calling the provider.

Run the suite after installing the compatible Numu reactor artifacts:

```sh
mvn -pl identity-access-service -am test -Dtoba.numu.identity=true
```

These tests exercise Toba's adapter boundary with a controlled SPI implementation. They do not replace an integration test against Numu's real persistence adapter; that should separately verify ACTIVE membership, SUSPENDED/REVOKED membership, and inactive-user behavior against the actual repository implementation.

## Milestone: in-process identity-to-membership authorization chain

The optional `NumuIdentityAuthorizationChainTest` exercises the combined
`VerifiedIdentityBridge.resolve` → `NumuTenantMembershipVerifier` →
`VerifiedIdentityBridge.requireActiveMembership` path. It verifies:

- an active membership is accepted for the verified canonical user and tenant;
- an inactive membership is rejected;
- a different requested tenant is rejected before calling Numu;
- asynchronous and synchronous membership-provider failures deny access.

This is an **in-process contract suite with a controlled Numu SPI**, not a
PostgreSQL-backed end-to-end test. Numu's PostgreSQL suite independently
tests its real persistence-backed `TenantMembershipResolver`. A full
cross-repository runtime test still requires packaging Numu's identity
adapter and its database in the same Quarkus test application (or a deployed
service boundary). Do not claim production integration from these tests alone.

Run after installing compatible Numu artifacts:

```sh
mvn -pl identity-access-service -am test -Dtoba.numu.identity=true
```

The default build remains independent of Numu:

```sh
mvn -pl identity-access-service -am test
```

Security invariants: only a verified upstream subject and issuer may enter
identity resolution; tenant membership does not imply lakehouse data grants;
unavailable or ambiguous identity/membership providers must never authorize.
