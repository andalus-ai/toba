# 05 — Roadmap and delivery plan

**Status convention:** `Draft committed` ≠ `merged` ≠ `build verified` ≠ `production ready`. All estimates below are sequencing proposals, not commitments.

| Wave | Milestone | Deliverables | Exit criteria | Status at 2026-10-20 |
|---|---|---|---|---|
| 0 | Foundation | Multi-module reactor, engine/ports/runtime separation | All modules compile; dependency rules enforced | Draft PR #25 reported; unverified |
| 1 | Query contracts | Authorization, semantic compiler and execution ports | Denied requests never execute; source switching rejected | Draft PR #26 reported; unverified |
| 2 | Execution adapter | Dremio/Toba provider integration | Contract tests for success, failure, timeout, status mapping | Draft PR #27 Dremio slice; unverified |
| 3 | Persistence | PostgreSQL jobs and migrations | Migration + CAS concurrency tests | Draft PR #28 initial slice; unverified |
| 4 | Runtime | Quarkus CDI, config, endpoints, embedded assembly | Quarkus startup + embedded parity | Draft PR #29; unverified |
| 5 | Durable submission | Persist-before-submit, fail-closed wiring, CI | Recovery cases tested; CI green | Draft PR #30; unverified |
| 6 | **Consolidation** | Merge/migrate existing BI query/governance code into engine/adapters; compatibility bridge | One authoritative query lifecycle; no parallel implementations | **Next priority** |
| 7 | **Worker hardening** | Fenced leases, audit, renewal, backoff, reconciliation | Concurrency and outage tests; no duplicate submissions | Planned |
| 8 | **Production runtime** | Authenticated REST, readiness, migrations, telemetry, operational controls | End-to-end Quarkus + PostgreSQL + stub Dremio CI | Planned |
| 9 | **Static BI MVP** | Semantic explorer, charts, dashboard authoring, immutable versions | Build/publish/reopen dashboard with scoped access | Planned |
| 10 | **Agentic BI MVP** | Agent planning, policy validation, dynamic chart specs, Batik A2UI | Prompt→governed chart→explanation, with injection tests | Planned |
| 11 | **Promotion** | Dynamic→static snapshot, review, versioning, refresh | Published version reproducible and independently refreshable | Planned |
| 12 | **Enterprise scale** | Schedules, collaboration, plugin SDK, quotas, multi-runtime proof | SLOs, compatibility tests, release/upgrade runbooks | Planned |

## Priority sequence to minimize rework

**Track A — Platform correctness (first):** confirm repository skeleton; stabilize Maven reactor; fix CI; reconcile legacy implementation; eliminate duplicate job models; migrate fenced worker; enforce identity and policy.  
**Track B — BI authoring (after stable query APIs):** semantic datasets, chart specification, dashboard versions and publishing.  
**Track C — Agentic interface (parallel after contract stabilization):** planner adapter, A2UI schema, chart generation, provenance and review.  
**Track D — Productization:** runtime packaging, examples, docs, upgrade path, operations and security review.

## Suggested release slices

### R0 — Developer preview

Embeddable Java engine + Quarkus app + governed query submission + persistent job lifecycle + reliable local development setup. **No public ungoverned SQL endpoint.**

### R1 — Conventional BI preview

Dataset catalog, chart specs, dashboard editing, static dashboard versioning and role-based publishing.

### R2 — Agentic BI preview

Natural-language semantic planning, policy-gated execution, chart/A2UI results, session history and explainability.

### R3 — Unified Farabi

Promotion of agent output to immutable static dashboard artifacts, refresh, review, collaboration, provenance and operational hardening.

## Cross-cutting definition of done

Each milestone requires: (1) build green, (2) unit + integration tests, (3) threat-model review for new trust boundaries, (4) docs and examples, (5) backward-compatibility statement, (6) operational telemetry, (7) clear migration/rollback steps, and (8) independently verifiable acceptance tests.

## Immediate backlog for next implementation batch

1. Confirm current `main` and draft PR lineage; fix stacked PR mergeability and build errors.
2. Map existing `tech.kayys.andalus.bi` code to Farabi domain/ports/adapters without parallel job lifecycles.
3. Define `ActorContext` and real governance/semantic adapters; remove placeholder deny-all only when production policy wiring is ready.
4. Unify query execution schema with existing BI migrations or provide explicit backfill/compatibility plan.
5. Restore fenced worker store, audit, recovery and scheduled polling under Quarkus lifecycle.
6. Add Testcontainers PostgreSQL and Dremio stub integration tests, Quarkus tests, ArchUnit module rules.
7. Ship one authenticated end-to-end governed query endpoint with job polling and result retrieval.
