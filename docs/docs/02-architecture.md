# 02 — Architecture and module contracts

## Architectural style

Farabi uses **hexagonal architecture (ports and adapters)** plus a **modular monorepo**. The domain and application layer define behavior and interfaces; infrastructure modules implement those interfaces; the runtime composes them. Dependencies point **inward**. Deployment topology must not leak into core APIs.

## Logical layers

| Layer | Responsibilities | Forbidden dependencies |
|---|---|---|
| Domain | Workspace-scoped identities, query lifecycle, semantic/chart/dashboard/artifact models, invariants | Quarkus, JDBC, HTTP, Dremio, UI libraries |
| Application | Query orchestration, visualization creation, artifact promotion, publication, policy gates | Concrete adapters and transport models |
| Ports/SPI | Repository, authorization, semantic compiler, execution, renderer/specification, agent planner, event/audit interfaces | Runtime-specific classes |
| Adapters | JDBC, Toba/Dremio, AI provider, Tenun spec mapper, Batik A2UI mapper | Runtime lifecycle assumptions |
| Runtime core | Library bootstrap, dependency composition, lifecycle hooks | Quarkus |
| Runtime Quarkus | CDI, config, REST, scheduling, metrics, readiness, security identity mapping | Core internals outside public contracts |
| Distribution | Deployable server, containers, packaging | Business logic |

## Recommended Maven modules (target, not all implemented)

```text
farabi/
  pom.xml
  farabi-bom/
  engine/
    domain/               # package-level initially; split Maven modules only if necessary
    application/
    port/
    spi/
  runtime-core/
  adapters/
    adapter-toba/
    adapter-dremio/
    adapter-jdbc/
    adapter-agent/
    adapter-tenun-spec/
    adapter-batik-a2ui/
  runtime-quarkus/
  distribution/server/
  examples/embedded-java/
  tests/contract/
  tests/integration/
  docs/
```

**Pragmatic migration rule:** keep existing `farabi/engine`, `farabi/runtime-core`, `farabi/runtime-quarkus`, `farabi/adapter-dremio` and `farabi/adapter-jdbc` paths where already established. Do not move directories solely to match this ideal tree until tests and downstream consumers are accounted for. Preserve legacy `tech.kayys.andalus.bi` APIs via compatibility adapters/deprecation windows.

## Key interfaces (illustrative)

```java
interface QueryAuthorizer { void authorize(QueryRequest request); }
interface SemanticQueryCompiler { QueryPlan compile(QueryRequest request); }
interface QueryExecutor { QuerySubmission submit(QueryPlan plan); }
interface QueryExecutionRepository { /* create, scoped find, fenced transitions */ }
interface DashboardRepository { /* save, version, load, publish */ }
interface VisualizationSpecPort { /* validate and normalize chart specs */ }
interface AgentPlannerPort { /* produce proposed analytical plans, not privileged SQL */ }
interface AuditPort { /* immutable decision and execution events */ }
```

The last four are **target contracts** and not claims that they exist today.

## Integration model

- **Toba** is the preferred data-platform boundary; direct Dremio adapter is an optional execution provider and should not bypass Toba governance where Toba is authoritative.
- **Tenun** integration exchanges renderer-neutral chart specifications with versioned Tenun-specific mappings. Rendering remains in the client/front-end or an explicitly selected rendering service.
- **Batik** integration exchanges structured A2UI view descriptions; the agent cannot inject arbitrary executable UI code.
- **AI** integration produces a typed proposal (`intent`, `dataset`, `metrics`, `dimensions`, `filters`, `chart`, `explanation`) validated by engine policies before query submission.

## Runtime and deployment

**Embedded:** host builds `FarabiRuntime` / `QueryRuntime`, supplies adapters, identity context, telemetry and lifecycle management.  
**Quarkus default:** CDI composition root, REST boundary, datasource, configuration, security identity, scheduling, health/readiness, OpenTelemetry and deployment packaging.  
**Workers:** can run co-located initially; scalable deployment uses independently configured worker processes with fenced leases and idempotent recovery.

## API boundaries

HTTP request bodies must not be trusted to supply `subjectId`, workspace authorization or unrestricted SQL. The runtime derives the principal from authenticated identity, resolves workspace membership, builds an engine request, and invokes a governed use case. Public API DTOs are not domain entities. Use stable versioned routes such as `/api/v1/workspaces/{workspaceId}/queries`, `/dashboards`, `/artifacts` when enabled.

## Compatibility and packaging

- Maven parent and BOM coordinate versions; each adapter is optional to embedded consumers.
- Avoid cyclic dependencies; enforce dependency rules in CI (e.g., module dependency tests/ArchUnit).
- Keep stable domain interfaces, introduce adapters for legacy APIs, and document breaking changes.
- Publish an embedded-Java example and a Quarkus server distribution from the same reactor.
