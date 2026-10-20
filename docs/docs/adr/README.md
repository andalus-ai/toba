# Architecture decision records

These decisions are **proposed/accepted in conversation** and should be ratified in repository review.

## ADR-001 — Modular monorepo

**Decision:** Keep Farabi in `andalus-developer/bi` with independently packaged Maven modules.  
**Reason:** Cross-module refactoring and CI coordination without separate repository overhead.  
**Consequence:** Enforce dependency boundaries with automated checks; a monorepo does not justify runtime coupling.

## ADR-002 — Library-first hexagonal engine

**Decision:** Pure Java engine owns domain/use cases/ports; adapters depend inward.  
**Reason:** Embed Farabi in other Andalus and Java applications.  
**Consequence:** No CDI, REST, JDBC, Dremio, chart renderer or LLM SDK types in core public contracts.

## ADR-003 — Quarkus default runtime

**Decision:** Quarkus provides default composition, HTTP, scheduling and operational services.  
**Reason:** Product-ready server without imposing a runtime on embedded users.  
**Consequence:** Prove runtime independence with an embedded example and parity tests.

## ADR-004 — Governed semantic queries only

**Decision:** Agent/client inputs become validated semantic plans; engine authorizes and compiles before executing.  
**Reason:** Prevent data-source bypass and untrusted SQL execution.  
**Consequence:** Fail closed until production identity and governance adapters are installed.

## ADR-005 — Dynamic-to-static as immutable versioned artifact

**Decision:** Promotion creates an independently owned, versioned dashboard snapshot.  
**Reason:** Reproducibility, review, sharing and stable refresh.  
**Consequence:** Store provenance, semantic references, renderer versions and publishing authorization.

## ADR-006 — Remote execution uses reconciliation

**Decision:** Persist intent, use idempotency where possible, and reconcile ambiguous remote submissions.  
**Reason:** Database and Dremio cannot share a single atomic transaction.  
**Consequence:** No blind resubmission; fenced worker transitions and audit are mandatory for production.

## ADR-007 — Toba, Tenun and Batik are replaceable integrations

**Decision:** Integrate through versioned ports and typed specs, not direct core dependencies.  
**Reason:** Keep the engine provider-agnostic and frontend-agnostic.  
**Consequence:** Build contract tests for each adapter and preserve explicit ownership boundaries.
