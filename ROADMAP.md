# Toba — Outcome-driven Roadmap

**Status convention:** proposed → implemented → build-verified → integration-verified → production-ready. A commit, branch or PR does not establish validation. Dates and sequencing are proposals.

| Phase | Focus | Minimum exit evidence |
|---|---|---|
| P0 | Stabilize current foundation | Reactor compiles; CI green; identity/catalog CDI wiring and deny/allow tests; no spoofable actor path |
| P1 | Lakehouse core | Governed catalog and Iceberg/storage operations; persistent metadata; schema/table contract and interoperability tests |
| P2 | Execution fabric | Dremio adapter conforms to engine SPI; durable jobs, status/results/cancel, capability checks, safe routing and recovery |
| P3 | Unified governance | Tenant/workspace/source/table policy consistency, audit, lineage, enforcement against bypass paths |
| P4 | Agentic lakehouse | Governed discovery, explain, plan and execute tools; approval and injection/failure tests |
| P5 | Optimization | Workload telemetry, bounded routing, acceleration/maintenance proposals, measured improvements |
| P6 | Ecosystem | Stable Farabi integration, SDKs, connector examples, extensibility and deployment docs |
| P7 | Enterprise hardening | HA/recovery, SLOs, performance/security tests, upgrades, backups and operational runbooks |

## Immediate priority

1. Finish verified catalog identity + Numu membership + authoritative grant adapters; validate both permitted and denied paths.
2. Run and repair Maven builds, Quarkus startup, CDI resolution and integration tests before claiming catalog readiness.
3. Reconcile catalog/Iceberg/storage APIs against engine-neutral core contracts.
4. Prove Dremio execution adapter lifecycle and durable Toba-owned query jobs with conformance tests.
5. Introduce governed agent-tool contracts early, even if advanced autonomous workflows follow later.
6. Audit and correct Farabi-specific documentation accidentally present under Toba's `docs/docs/`.

## Cross-cutting definition of done

Every completed feature requires reproducible build; unit, integration and security tests appropriate to the trust boundary; documented public contract; operational signals; failure/retry semantics; compatibility/migration note; and verified acceptance criteria. Mark unsupported capabilities explicitly. Do not replace working safeguards with mocks or hardcoded allow decisions.

## Delivery policy

Prefer cohesive, reviewable batches over many small PRs. Keep a feature branch until its milestone is validated. Do not open a PR merely because several files were committed; obtain explicit user approval or follow the established consolidated-review checkpoint.
