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
