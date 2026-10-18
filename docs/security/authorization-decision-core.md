# P1.5 — Authorization decision core

The identity module now contains a pure `AuthorizationDecisionService` with an immutable `AuthorizationRequest` record. The evaluator denies requests unless a non-null principal, exact tenant membership, optional exact workspace membership, nonempty action/resource and explicit action grant are present. Wildcard grants do not imply permission.

## Trust boundary

**This evaluator is not an authorization enforcement point.** Every field is currently supplied by its caller. In particular, a caller could forge `grantedActions`, `membershipTenantId`, or `membershipWorkspaceId`. Therefore no REST resource, gRPC handler or agent tool may treat a caller-constructed request as authoritative.

Before production integration:

1. Load grants and memberships from the authoritative identity database using the authenticated principal (never request headers or user-supplied claims).
2. Resolve tenant/workspace and the resource's owning scope server-side; validate the principal's current membership and revocation status.
3. Add resource-specific policies (ownership, role assignments, attribute conditions and policy version), not only action-level grants.
4. Enforce decisions at every protected entry point and recheck when relevant policy or membership changes.
5. Return an auditable decision ID and reason code without disclosing cross-tenant resource existence.
6. Add tests for forged inputs, revoked tokens, concurrent role changes, service-account permissions, resource ownership and database unavailability.

Unit tests cover only the pure decision logic. No Maven build or integration tests were executed during this increment.
