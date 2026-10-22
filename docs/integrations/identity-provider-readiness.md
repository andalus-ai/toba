# External identity provider startup gate

The identity-access-service supports an optional strict startup gate:

```properties
toba.identity.require-providers-at-startup=true
```

When enabled, application startup fails unless CDI discovers exactly one
`ExternalIdentityResolver` and exactly one `TenantMembershipVerifier`.
Missing or ambiguous implementations are configuration errors.

The default is `false` to preserve standalone deployments without Numu.
Runtime authorization still fails closed if a provider is missing or ambiguous;
the default does **not** permit tenant access without membership verification.

For Numu deployments, install compatible Numu artifacts and activate
`-Dtoba.numu.identity=true` at Maven build time. This enables the optional
membership adapter, but does **not** supply an external identity resolver.
Configure an authenticated, trusted resolver separately before enabling the
strict startup gate.

Validation:

```sh
mvn -pl identity-access-service -am test
mvn -pl identity-access-service -am test -Dtoba.numu.identity=true
```

The second command requires compatible Numu Maven artifacts. Neither command
establishes a real PostgreSQL-backed cross-repository end-to-end test.

The startup gate checks bean availability, not database health or token
validation. Requests must still validate credentials upstream and enforce
Toba-specific catalog/query permissions independently of tenant membership.
