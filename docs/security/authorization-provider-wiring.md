# P1.7 — Authorization provider wiring (safe default)

This increment adds a CDI-managed `AuthorizationEvidenceProvider` fallback and a producer for `TrustedAuthorizationResolver`. The fallback returns no authorization evidence and therefore **denies every request**. This is intentional: no production grant may be inferred while authoritative identity storage is not connected.

## Not yet implemented

This is **not a database-backed authorization provider**. The existing role-assignment and permission repositories must be inspected for exact signatures, scope hierarchy semantics, reactive transactions and revocation behavior before connecting them. No REST or gRPC handler currently calls the resolver. The integration tests have not been run.

## Next steps

1. Replace the fallback bean with a single authoritative provider implementation; avoid CDI ambiguity by removing the fallback or selecting with a mutually exclusive build-time profile.
2. Resolve current principal status, tenant membership, workspace membership, applicable role assignments and permission grants from authoritative repositories. Do not trust caller-supplied grants.
3. Use reactive persistence APIs, not blocking database access on the event loop.
4. Check resource ownership and required action, and invoke the resolver from protected REST and gRPC operations.
5. Add tests for role revocation, permission-store failure, cross-tenant access and service accounts.

**Deployment:** with the fallback active, any future route wired to the resolver will deny all requests. Do not disable it by inserting a permissive or synthetic evidence provider.
