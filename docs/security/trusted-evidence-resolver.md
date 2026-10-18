# P1.6 — Trusted authorization evidence boundary

This increment adds a server-side resolver orchestration contract, not a database-backed permission enforcement point.

- `AuthorizationEvidenceProvider` is an interface. **There is no production implementation yet.**
- `TrustedAuthorizationResolver` accepts an authenticated principal UUID and calls the provider, rejecting missing, inactive, mismatched, or failed evidence. It then delegates to the P1.5 decision evaluator.
- The evidence provider must derive grants from authoritative, current role assignments and membership tables, not HTTP headers, arbitrary JWT permission claims, or caller-submitted fields.
- No REST/gRPC route currently invokes the resolver. No tenant or resource authorization is enforced by adding these classes alone.

## Required integration

1. Identify and verify the actual user, role-assignment, permission and tenant membership repositories and their reactive APIs.
2. Implement an asynchronous, database-backed evidence provider using those repositories. Do not block Quarkus event-loop threads.
3. Verify resource ownership and scope server-side, and resolve the authenticated principal through a trusted identity mapping.
4. Enforce the resolver at all protected REST/gRPC operations; remove reliance on the legacy actor header.
5. Add integration tests for role revocation, tenant switching, scope inheritance, database outages and service identities.
6. Run Maven tests and a full build in CI before merging.

**Security note:** the pure resolver catches runtime failures and denies, but an untrusted provider could return fabricated evidence. Trust comes from the future provider implementation and its secure wiring, not from this interface.
