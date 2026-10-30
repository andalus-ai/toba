# Toba — Architecture and Enforceable Boundaries

**Status:** Target architecture and development constraints; consult source and tests for implemented features.

## Structural principle

Use a domain-first hexagonal architecture: **domain → application/use cases → ports/SPI ← adapters ← runtime/composition**. Dependencies point inward. Quarkus is a preferred runtime adapter, not a domain dependency. Avoid refactors that merely rearrange folders without improving verified boundaries.

```text
Clients / Farabi / SDKs / Governed Agents
                 |
      API / Authenticated Runtime
                 |
         Toba Application Core
   Catalog | Query Jobs | Governance
   Workloads | Agent Tools | Optimization
                 |
           Stable Ports / SPI
       /         |          \
 Dremio Adapter  Catalog/Storage  Numu/Policy Adapters
       |         |          |
  Execution    Data/Metadata  Trusted Evidence
```

## Six platform capabilities

1. **Lakehouse core:** catalog, metadata, namespaces, tables, snapshots, schema evolution, storage and Iceberg interoperability.
2. **Execution fabric:** capability negotiation, query submission/status/results/cancellation, workload routing and engine adapters.
3. **Governance/security:** verified identities, tenant/workspace isolation, authoritative grants, row/column controls, lineage and audit.
4. **Intelligence/agents:** discover/plan/explain/diagnose tools with typed inputs, bounded effects, approval and audit.
5. **Optimization fabric:** workload observation, cost-aware routing, acceleration and safe maintenance.
6. **Integration/experience:** versioned APIs, SDKs, operational interfaces, Farabi integration and deployment packaging.

## Core versus adapters

- Core defines Toba-owned IDs, use cases, policies, invariants and contracts; it must not import Dremio, Quarkus, Numu, Farabi or model-provider SDKs.
- Engines implement the engine SPI and declare capabilities explicitly; never assume uniform support for cancellation, transactions, profiles, parameters or time travel.
- Catalog and storage integrations are replaceable; provider-specific metadata must not silently become mandatory public fields.
- Authentication adapters derive canonical user/tenant context from verified credentials. Client-supplied actor IDs, grant lists and membership claims are not authoritative.
- Farabi consumes Toba's governed interfaces. No Toba core dependency on Farabi charts, dashboards, Tenun or Batik.
- AI provider output is untrusted proposal data. Policy enforcement and execution decisions remain server-side.

## Mandatory query lifecycle

1. Authenticate verified credentials; resolve canonical actor, tenant and workspace.
2. Resolve authoritative membership and grants, including source/table/column and row-level restrictions as applicable.
3. Validate typed query/semantic plan, SQL boundaries, quotas, resource limits and capabilities.
4. Persist Toba-owned job state before submission; use idempotency/fencing and reconcile ambiguous outcomes.
5. Route to a compatible engine; keep provider job ID separate from Toba job ID.
6. Reauthorize scoped status/results/cancel operations; emit auditable transitions and sanitized diagnostics.
7. Enforce at engine/source boundary or block direct bypass paths. Control-plane checks alone do not secure an externally accessible engine.

## Mandatory agent lifecycle

- Expose only allowlisted, typed and versioned tools.
- Authenticate agent identity and delegated user scope; use least privilege and explicit time-bound delegation.
- Distinguish read-only inspection, proposed mutations and approved execution.
- Require policy checks, budget limits, dry-run/preflight where applicable, human approval for high-risk operations, and immutable audit evidence.
- Make retries idempotent and compensations/rollback explicit where possible.
- Treat retrieved metadata, prompts and tool responses as untrusted; never interpret them as authority to change permissions.

## Engineering and deployment

- Contract-test engine adapters and catalog/storage adapters; integration-test Dremio against a pinned supported version.
- Verify reactive/CDI wiring in runtime tests; unit mocks alone do not prove startup or allow-path correctness.
- Define API and schema compatibility, migrations, telemetry, readiness, secrets handling and failure modes.
- Keep the Java 17 engine SPI dependency-free unless an ADR explicitly changes this constraint.
- Prefer additive compatibility adapters over breaking public contracts.
- A deny-only placeholder is a secure interim state, **not** a completed feature.

## Documentation scope warning

Some legacy files under `docs/docs/` currently describe **Farabi BI**. They are not authoritative for Toba's product mission or module ownership. Until audited and relocated/corrected, use `VISION.md`, `ARCHITECTURE.md`, `ROADMAP.md`, root `AGENTS.md` and Toba-specific `docs/architecture/engine-strategy.md` for Toba decisions. Do not silently rewrite historical documents; track cleanup separately.
