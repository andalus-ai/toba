# 03 — Core workflows and domain model

## Governed query lifecycle

1. Authenticate caller in the runtime; derive trusted identity and workspace membership.
2. Resolve semantic dataset/metric and authorize data access.
3. Compile a bounded, parameterized query plan; validate source, columns, filters, cost and limits.
4. Persist submission intent with a unique request/idempotency key.
5. Submit to execution provider and persist provider job ID.
6. Poll or receive provider status; update with fenced compare-and-set transitions and audit.
7. Return scoped results or an actionable error; retain provenance and cost metadata.

**State model:** `PENDING → SUBMITTED → RUNNING → {SUCCEEDED, FAILED, CANCELLED}`; failure/cancellation may occur earlier. Ambiguous remote acceptance is an explicit **reconciliation** state/condition, not a signal to resubmit automatically. Provider state mapping must tolerate unknown states without falsely marking failure.

## Conventional dashboard lifecycle

`Dataset → Semantic model → Query specification → Chart specification → Layout → Draft dashboard → Validation → Published version → Refresh / revision`.

Dashboard artifacts are immutable by version. The latest published pointer can change without rewriting previous versions. All charts reference stable semantic and dataset identities, not only raw SQL strings.

## Agentic exploration lifecycle

`Prompt → Intent extraction → Candidate semantic plan → Policy/validation → Query → Result summary → Chart spec → Batik A2UI view → User refinement`.

The agent is a **planner**. The engine owns validation, policy and execution. An explanation should disclose relevant filters, metric definitions, freshness, limitations and query provenance.

## Dynamic-to-static promotion (core differentiator)

1. User selects a dynamic result/chart/dashboard composition.
2. Engine freezes validated chart specs, query definitions, semantic model references, parameters, layout and presentation metadata.
3. Re-run authorization under the publishing identity; optionally require steward review.
4. Persist an immutable artifact version with lineage, creation time, author and reproducibility metadata.
5. Publish to an authorized audience; schedule refresh independently of artifact definition.
6. Future agent sessions may propose new revisions, but never silently mutate a published version.

**Artifact manifest (proposed):** `artifactId`, `version`, `workspaceId`, `ownerId`, `title`, `layout`, `visualizations[]`, `semanticRefs[]`, `dataSourceRefs[]`, `parameterBindings`, `rendererSpecVersion`, `a2uiSpecVersion`, `provenance`, `policySnapshotRef`, `createdAt`, `publishedAt`, `checksum`.

## Domain aggregates (target)

- **Workspace:** tenant boundary and membership.
- **SemanticAsset:** governed datasets, dimensions, metrics and model version.
- **QueryExecution:** request identity, state, provider mapping, policy decision, provenance and lifecycle.
- **Visualization:** renderer-neutral chart specification, dataset binding and interactions.
- **Dashboard:** layout, filters, linked visualizations and draft metadata.
- **DashboardVersion:** immutable snapshot with provenance and publishing state.
- **AgentSession:** prompts, validated plans and ephemeral views; promotion creates an independent dashboard artifact.

## Data ownership

Farabi stores metadata, policies/decisions references, query lifecycle and artifact definitions. Toba and upstream platforms own underlying datasets. Result caches should be explicitly configured, encrypted, scoped, expired and never mistaken for the authoritative data lake.

## Consistency and recovery

A database transaction cannot atomically include a remote Dremio submission. Persist intent first; distinguish 'not attempted', 'possibly accepted' and 'confirmed provider ID'. Use idempotency where the provider supports it; otherwise investigate ambiguous submissions through provider correlation rather than blind retries. Worker transitions require lease owner, epoch/fencing token, expected state and atomic audit. Scheduled refresh uses the same governed query pipeline.
