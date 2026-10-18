# Current repository gaps — initial audit

This is a source/documentation audit, not a claim that the Maven build or deployed system was executed.

## Confirmed by source inspection

- Root Maven coordinates and module documentation retain the historical `dremio-reactive-platform` artifact ID. This PR changes the product name/description while preserving coordinates to avoid breaking child POMs.
- Root `pom.xml` contains stale comments describing several modules as not yet scaffolded even though they are listed in the reactor.
- README references a non-existent `dremio-reactive-extension-plan.md`; the existing plan is `toba-extension-plan.md`.
- Identity service README states `X-Actor-Id` is a placeholder and must not be treated as authenticated.
- Identity service README states JWT signing/issuance and JWT principal resolution are incomplete.
- Governance service README states masking-policy gRPC lookup and tests are not implemented; definitions alone do not enforce masking.
- Catalog service README describes an Iceberg REST subset with no commit-transaction endpoint and no snapshot/time-travel endpoints.
- Dremio adapter reconciliation notes say the actual Dremio-side StoragePlugin implementation is not built; mirrored interfaces are not deployable proof of compatibility.
- Workload orchestration README says real Dremio queue-depth telemetry and the job-submission routing hook are missing.
- Semantic AI README says REST endpoints remain unauthenticated while MCP is secured.
- No live Dremio integration, complete cross-service authorization tests, or release-level enterprise acceptance evidence is established by these README statements.

## Immediate implication

Treat the current system as an active engineering prototype. The next work must prioritize verified identity, default-deny API security, governance enforcement in the execution path, a reproducible CI build, and real integration tests—not add more agent features before those controls exist.
