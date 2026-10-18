# Toba enterprise-readiness roadmap

This roadmap distinguishes existing code from production guarantees. The presence of a service/module is not proof that its security, reliability or conformance properties are complete.

## P0 — Truthful baseline and boundaries
- Maintain one canonical architecture/roadmap document.
- Build all modules in CI with pinned Java/Maven and reproducible dependency resolution.
- Publish module-level capability and known-gap status.
- Define engine SPI and contract tests.
- Inventory unauthenticated routes, placeholder identity headers, fail-open behavior, secrets, direct-engine bypass paths and missing integration tests.

## P1 — Identity and policy enforcement
- Replace `X-Actor-Id` placeholders with verified request identities.
- Complete JWT signing/rotation and token verification; support OIDC federation before enterprise rollout.
- Secure every REST endpoint, not only MCP; deny by default.
- Add tenant isolation, scoped RBAC/ABAC, service identities and secret rotation.
- Implement governance policy lookup in the execution path and test row/column controls.
- Make audit append-only, tamper-evident, actor-attributed and queryable under authorization.

## P2 — Reliable engine execution
- Dremio adapter conformance against a pinned OSS revision.
- Idempotent submission, durable job ownership, outbox/reconciliation and crash recovery.
- Timeout, cancellation, bounded result streaming, pagination and sanitized profiles.
- Real workload telemetry and routing; remove manual-only queue-depth inputs.
- Contract tests and integration tests using an isolated engine deployment.

## P3 — Lakehouse correctness
- Complete Iceberg REST catalog commit/transaction behavior.
- Test optimistic concurrency, metadata atomicity, schema/partition evolution and object-store consistency.
- Snapshot/time-travel operations, retention, compaction and orphan-file cleanup.
- Backup/restore and recovery exercises.

## P4 — Agentic data operations
- Tool registry with typed schemas, tenant-scoped authorization and audit for every call.
- Separate read-only tools from mutating tools; approval gates for policy changes, writes, deletes, and infrastructure actions.
- Query planner that emits validated semantic plans rather than unrestricted SQL by default.
- Prompt-injection-resistant handling of catalog descriptions and retrieved content.
- Evaluation suite for correctness, permission leakage, cost and refusal behavior.
- Human review for generated metadata and policy proposals.

## P5 — Enterprise operations
- OIDC/SAML/LDAP as required by target customers, secrets manager integration, TLS and key rotation.
- HA, service SLOs, backup/restore, disaster recovery and upgrade/rollback procedures.
- OpenTelemetry metrics/traces/logs, audit retention, alerting and incident runbooks.
- Load, chaos, security, dependency, SBOM and container-image scanning.
- Kubernetes least-privilege RBAC and network policies.

## P6 — Native Andalus BI integration
- Stable catalog/semantic/query/job APIs with user identity propagation.
- Dataset discovery, semantic metrics, query results and lineage.
- Dynamic A2UI-generated dashboard to reviewed/published static dashboard artifact.
- End-to-end tests proving BI cannot bypass Toba authorization.

## Release gate

Do not claim enterprise-ready until security controls are enforced in the real execution path, module builds and integration tests pass in CI, recovery and restore are demonstrated, and deployment guidance has been exercised against a supported environment.
