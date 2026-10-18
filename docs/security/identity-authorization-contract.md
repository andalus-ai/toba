# Identity REST authorization contract (P1.4)

Status: design constraints and initial actor validation tests, **not a completed authorization implementation**.

## Identity invariants

1. Derive actor identity only from a verified Quarkus `SecurityIdentity`. Never use `X-Actor-Id` as authority.
2. Treat JWT subject as an external identifier until a trusted local mapping has been established. Reject unmapped subjects; do not silently create identities.
3. Resolve the active tenant and workspace from verified membership or trusted service credentials, not caller-selected headers alone.
4. For each mutation, authorize the operation and its target scope before invoking the service or writing an audit event.
5. Return 401 for unauthenticated requests and 403 for authenticated callers lacking permission. Do not leak cross-tenant object existence.
6. Re-evaluate authorization for resource reads and administrative operations; a valid token does not imply read or manage permission.
7. Treat service accounts and PATs as independently revocable principals with explicit scopes.
8. Fail closed when role, membership, or governance policy cannot be resolved.

## Implementation checklist

- Replace legacy header parameters in all REST resources with a request-scoped verified principal provider.
- Bind local JWTs to an immutable user ID and tenant membership, and validate issuer, audience, expiry and signing key.
- Add a trusted federation subject mapping for OIDC/LDAP identities.
- Require action + resource scope in an authorization service; do not infer permissions from authentication.
- Add negative tests: forged actor ID, expired token, revoked principal, tenant boundary, role downgrade, permission service unavailable.
- Ensure public login and health endpoints are explicitly enumerated and cannot overlap management endpoints.

## Current limits

The existing `VerifiedActorFilter` is a temporary compatibility shim. The tests in this increment exercise UUID subject validation only. They do not prove end-to-end actor binding, tenant isolation, or resource-level authorization. Do not promote this service as enterprise-ready until the checklist is implemented and integration tests pass.
