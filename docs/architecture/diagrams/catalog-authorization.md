# Catalog authorization and actor provenance — target

```mermaid
sequenceDiagram
  autonumber
  actor Client
  participant REST as Catalog REST
  participant Guard as CatalogAccessGuard
  participant Auth as VerifiedCatalogAuthorizationProvider
  participant Identity as Identity Verifier
  participant Grants as Membership + Grants Verifier
  participant Service as Catalog Service
  Client->>REST: Catalog operation
  REST->>Guard: Authorize(action, resource, context)
  Guard->>Auth: Authorize request
  Auth->>Identity: Verify authenticated credentials
  Identity-->>Auth: Canonical user, tenant, workspace
  Auth->>Grants: Verify membership and authoritative grants
  alt Missing provider, invalid evidence, revoked or denied
    Grants-->>Auth: Deny / error
    Auth-->>Guard: Forbidden
    Guard-->>REST: Forbidden
    REST-->>Client: Access denied
  else Explicit allow
    Grants-->>Auth: Allowed
    Auth-->>Guard: Canonical actor UUID
    Guard-->>REST: Canonical actor UUID
    REST->>Service: Execute with verified actor
    Service-->>Client: Scoped result
  end
```

**Security rule:** Never accept `X-Actor-Id`, client grants or unverified principal UUIDs as authority. **Status:** composition SPI exists; concrete trusted identity and membership/grant adapters still require implementation and tests.
