# P1 identity authorization — implementation notes

This branch adds a fail-closed management guard to user, role, role-assignment, service-account, and personal-access-token REST routes.

## Deployment prerequisite

Set `toba.identity.management-scope-path` to the canonical scope where the trusted identity administrator is granted permissions. Without this setting management requests fail closed (HTTP 503). Seed initial grants out of band through a trusted bootstrap process; do not expose an unauthenticated bootstrap endpoint.

Required permission keys: `identity.user.read`, `identity.user.manage`, `identity.role.read`, `identity.role.manage`, `identity.role-assignment.manage`, `identity.service-account.read`, `identity.service-account.manage`, and `identity.pat.manage`.

PAT issuance and listing are restricted to the authenticated USER principal. PAT revocation verifies ownership inside the service transaction. Service-account PAT issuance is intentionally not exposed by this endpoint.

## Security gaps to close before production

- **Privilege escalation:** role grants must validate the role's permission set against the grantor's delegable permissions, not only `identity.role-assignment.manage`.
- **Tenant isolation:** user/role/service-account repositories still need authoritative tenant filters; global listing endpoints are not tenant-safe.
- **gRPC:** callers need independently verified workload identities, not self-asserted principal IDs.
- **PAT:** permission scope, maximum lifetime, revocation concurrency and user-disabled checks need end-to-end enforcement.
- **Tests:** integration tests for 401/403, cross-tenant access, PAT ownership and role delegation are required.
- **Scope grammar:** normalize and validate canonical scope segments before storing or evaluating grants.

**This is an incremental security-hardening PR, not a production-ready authorization boundary. Keep it in draft until the gaps above are addressed and CI passes.**
