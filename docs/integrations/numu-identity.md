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
