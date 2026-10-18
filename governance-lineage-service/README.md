# Governance & Lineage Service

Phase 2 of the roadmap in `dremio-reactive-extension-plan.md`. Owns masking-policy
definitions, the lineage graph, and the append-only audit trail.

## Status

| # | Layer | Status |
|---|---|---|
| 1 | Domain model + migration | ✅ done |
| 2 | Repository layer | ✅ done |
| 3 | Service layer | ✅ done |
| 4 | REST layer | ✅ done |
| 5 | Kafka audit-ingestion consumer (`identity.events`) | ✅ done |
| 6 | gRPC masking-policy lookup (inline call from `dremio-adapter`) | not started |
| 7 | Tests | not started |

## What's built

- **`MaskingPolicy`** — row/column masking policy definitions. This service only
  ever stores and serves definitions (`GET /api/v1/masking-policies/applicable`) —
  the actual Calcite `RelNode` rewrite happens in the (not-yet-built) `dremio-adapter`.
  `expression` goes through a conservative denylist check on write (rejects `;`,
  `--`, DDL/DML keywords) since this service persists a raw SQL fragment it never
  itself executes; full syntactic validation is the adapter's job, since it has the
  Calcite parser.
- **`LineageEdge`** — flat source/target edges (table- or column-level). One-hop
  upstream/downstream queries are built; full-graph recursive-CTE traversal is
  intentionally deferred to whichever REST endpoint first needs multi-hop closure
  (see plan's note that this can move to a graph DB if traversal scale outgrows it).
- **`AuditLogEntry`** — append-only, populated by `AuditIngestionConsumer`, a Kafka
  listener on `identity.events` that mirrors every identity domain event into an
  audit row, idempotent on `sourceEventId` (Kafka is at-least-once). Query surface:
  by resource path or by actor, both time-ranged, matching the compliance-evidence
  use case (plan Section 5.2 / Section 11 Phase 6).

## Known gaps / next continuation prompts

- **Only `identity.events` is consumed.** `catalog.events` and `dremio.job-events`
  consumers follow the exact same `AuditIngestionConsumer.ingest(...)` shape once
  the Catalog Service and the `dremio-adapter` exist and publish to those topics.
- **No gRPC masking-policy lookup yet** — the adapter will need this synchronous
  (not just REST-pollable) per the plan's "gRPC for latency-sensitive paths" rule;
  the REST `/applicable` endpoint is a stand-in until then.
- **Lineage edges aren't populated automatically** — nothing yet consumes Calcite
  plan JSON to derive edges; `POST /api/v1/lineage/edges` exists for manual/backfill
  use and as the target shape a future plan-parsing consumer will call into.
- **No tests yet.**

Suggested next continuation prompt: **"Build the Catalog Service's Iceberg REST
endpoints"** (already underway alongside this module — see `catalog-service/`) or
**"Add gRPC masking-policy lookup to governance-lineage-service"**.
