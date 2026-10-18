# Toba is Extending Dremio OSS with Enterprise-Grade Capabilities
## Implementation Plan & Architecture (Quarkus Reactive Microservices)

---

## 1. Goal & Approach

The goal is to close the gap between **Dremio Community (OSS)** and **Dremio Enterprise/Cloud** by adding a layer of **new, independently-deployable Quarkus reactive microservices** around the existing Dremio OSS core — not by forking and rewriting the query engine.

Guiding principle: **strangler-fig, not rewrite.**
Dremio's coordinator/executor engine (Calcite planner + Arrow-based Sabot execution) is a mature, highly optimized distributed SQL engine. Reimplementing it is out of scope and not valuable. What's missing in OSS is almost entirely **platform/control-plane capability**: governance, fine-grained security, catalog services, elastic workload management, and an AI semantic layer. Those are naturally service-oriented, stateful-but-isolated concerns — a good fit for Quarkus reactive microservices sitting alongside Dremio rather than inside its monolith.

---

## 2. Current State: Dremio OSS Architecture (as-is)

From the `dremio-oss` repo structure:

| Module | Responsibility |
|---|---|
| `sabot` | Core execution engine — Arrow-based columnar vectorized execution, RPC fabric, operators |
| `dac` (Dremio Analyst Center) | Coordinator node, REST API, UI backend, job management, catalog/namespace, KVStore |
| `services` | Cross-cutting services: namespace, jobs, accelerator (reflections), scheduler, users |
| `plugins` / `connector` | Source connector SPI (S3, JDBC sources, Hive, Iceberg, etc.) |
| `protocol` | RPC/wire protocol definitions |
| `provision` | YARN/Kubernetes executor provisioning |
| `client` | JDBC/ODBC/Arrow Flight SQL clients |
| `ui` | React-based web console |

Key architectural facts that shape the plan:
- Single JVM monolith per node (coordinator vs executor role), Calcite-based cost-based optimizer, Arrow Flight SQL as the primary wire protocol.
- Metadata/namespace persisted in an embedded **KVStore** (RocksDB locally, or pluggable) — not a relational DB.
- No built-in RBAC beyond coarse read/write ACLs; no external IdP, no row/column masking, no lineage, no autonomous reflections, no elastic engine scaling, no hosted Iceberg REST catalog — these are the exact items Enterprise/Cloud add on top.
- Extension points already exist: `StoragePlugin` SPI, `AccessControl`/`AccessChecker` interfaces, REST API (`dac/backend`) is a normal JAX-RS app that can be fronted or extended, and job/reflection events are observable via listeners.

---

## 3. Feature Gap Analysis (OSS → Enterprise/Cloud)

Based on Dremio's published edition matrix, the concrete deltas to target, grouped by the services in Section 5:

| Category | Missing in OSS | Target Service |
|---|---|---|
| **Governance** | Data lineage, audit history, row/column-level access control, external policy providers (Ranger/Privacera) | Governance & Lineage Service |
| **Identity/Access** | Personal access tokens, external LDAP/OIDC, RBAC, custom roles, service users, external token providers | Identity & Access Service |
| **Catalog** | Hosted Iceberg REST catalog, time travel, auto-optimization/clustering, external engine attach | Catalog Service (Iceberg REST-compliant) |
| **Query Engine** | Autonomous reflections (self-tuning acceleration) | Reflection Advisor Service |
| **AI Semantic Layer** | AI-enabled semantic search, AI-generated wikis/labels, AI functions, agent/MCP integration | Semantic AI Service |
| **Platform** | Out-of-box monitoring, custom workload management (WLM), engine auto-start/stop, replica scaling, intracluster encryption | Workload Orchestration Service |
| **Compliance** | Audit trail feeding SOC2/HIPAA/GDPR reporting | Audit Service (shared by Governance) |

This table is the backlog — each row becomes one or more epics in Section 11.

---

## 4. Target Architecture

```
                         ┌─────────────────────────┐
                         │   API Gateway / BFF      │  (Quarkus reactive, REST + GraphQL)
                         │  authN/authZ, routing    │
                         └───────────┬─────────────┘
        ┌───────────────┬───────────┼───────────────┬───────────────┐
        │               │           │               │               │
┌───────▼──────┐ ┌───────▼──────┐ ┌─▼────────────┐ ┌▼──────────────┐ ┌▼───────────────┐
│  Identity &   │ │ Governance & │ │  Catalog     │ │  Workload      │ │  Semantic AI    │
│  Access Svc   │ │ Lineage Svc  │ │  Service     │ │  Orchestrator  │ │  Service        │
│ (RBAC, SSO,   │ │ (audit, row/ │ │ (Iceberg     │ │ (elastic       │ │ (embeddings,    │
│  PAT, SCIM)   │ │  col mask,   │ │  REST cat.,  │ │  engines,      │ │  AI functions,  │
│               │ │  lineage)    │ │  time travel)│ │  WLM, autoscale│ │  MCP, agent)    │
└───────┬───────┘ └──────┬───────┘ └──────┬───────┘ └───────┬────────┘ └────────┬────────┘
        │                │                │                 │                   │
        └────────────────┴───────┬────────┴─────────────────┴───────────────────┘
                                  │  event bus (Kafka/Pulsar) + shared outbox
                          ┌───────▼────────┐
                          │  Dremio OSS     │   (unmodified core: coordinator + executors)
                          │  Coordinator/   │   sabot engine, Calcite planner, Arrow Flight SQL
                          │  Executor nodes │   extended via StoragePlugin/AccessControl SPI hooks
                          └─────────────────┘
```

Design rules:
1. **Dremio OSS core stays authoritative for query planning/execution.** New services never re-implement SQL execution; they *inform* it (via SPI hooks, catalog metadata, access-control callbacks) or *sit beside it* (catalog, identity, AI).
2. **Each new capability is its own Quarkus reactive microservice** — independent deployable, independent datastore, independent scaling — communicating via REST/gRPC synchronously and an event bus asynchronously.
3. **Integration with Dremio's Java SPI happens through thin adapter modules** compiled against `dremio-oss` interfaces (`AccessControlChecker`, `MetadataPolicy`, `StoragePluginConfig`, catalog listeners) that delegate out to the new services over gRPC — this is how OSS calls into the new platform without being forked internally.

---

## 5. Microservice Breakdown

### 5.1 Identity & Access Service
- OIDC/SAML/LDAP federation (via Keycloak as embedded IdP broker, or direct OIDC client), SCIM provisioning, Personal Access Tokens, service-account/service-user identities, custom role definitions (RBAC/ABAC hybrid).
- Issues short-lived signed tokens Dremio's coordinator validates on every request (JWT + JWKS), replacing/augmenting Dremio's local auth.
- Reactive stack: `quarkus-oidc`, `quarkus-keycloak-admin-client`, `quarkus-hibernate-reactive-panache` + Postgres for role/policy storage.

### 5.2 Governance & Lineage Service
- Column/table-level **lineage graph** built by consuming Dremio job-plan events (Calcite plan JSON) off the event bus; stores lineage as a graph (Postgres + recursive CTEs, or a graph DB like JanusGraph/Neo4j if scale requires it).
- **Row/column masking policies**: evaluated as a gRPC callback that the `AccessControlChecker` adapter in Dremio invokes at plan time — masking is injected as a Calcite view filter/`CASE` rewrite before execution (this mirrors how Enterprise implements it).
- **Audit history**: consumes every job/query/DDL event, persists immutable audit log, exposes query API for compliance reporting (SOC2/GDPR/HIPAA evidence).
- External policy provider adapters (Ranger/Privacera) as pluggable policy-source connectors.

### 5.3 Catalog Service (Iceberg REST Catalog)
- Implements the **Apache Iceberg REST Catalog spec** (a well-defined OpenAPI contract) — this is the single highest-leverage service since it's a spec-compliant, greenfield build, not a Dremio-internal integration.
- Adds: catalog hosting, time travel (snapshot listing/rollback), automatic table optimization & compaction (background Quarkus scheduled/reactive jobs using Iceberg's `RewriteDataFiles`/`ExpireSnapshots` actions), clustering hints, and "external engine" attach (Spark/Trino/Flink can read the same catalog).
- Dremio OSS attaches to it exactly the way it attaches to any Iceberg REST catalog today (`IcebergRestCatalog` `StoragePlugin` — this already exists in OSS as a client capability), so no core changes needed here, only the new catalog server.
- Stack: `quarkus-rest-reactive` implementing the Iceberg REST OpenAPI contract, Postgres for catalog metadata, object storage (S3/ADLS/GCS) for table data — reuse `iceberg-core` Java libs.

### 5.4 Workload Orchestration Service
- Elastic "engine" concept: logical pools of executor pods with **auto-start/stop**, **min-replica=0**, and **replica autoscaling** based on queue depth/queued-query metrics.
- Talks to Kubernetes API (`quarkus-kubernetes-client`) to scale executor `StatefulSet`/`Deployment` replicas per engine/workload class.
- Custom WLM: query-tagging + priority queues enforced by intercepting job submission (adapter hook in `dac` job submission path) and routing to the right engine pool.
- Exposes monitoring metrics (Micrometer → Prometheus) that OSS doesn't ship out of the box.

### 5.5 Semantic AI Service
- Embedding pipeline: on catalog/wiki/schema change events, generate embeddings (pluggable LLM provider — OpenAI/Anthropic/Gemini/Bedrock, "bring your own model" like Enterprise) for semantic search over datasets/columns/wikis.
- **AI-generated wikis & labels**: reactive job that proposes descriptions for tables/columns from schema + sample data + query history, stored as suggestions for human approval (keeps humans in the loop, avoids silently mutating metadata).
- **AI functions**: exposes SQL-callable UDFs (`AI_SUMMARIZE`, `AI_CLASSIFY`, etc.) implemented as a Dremio UDF that proxies to this service.
- **MCP server / agent tools**: exposes the semantic catalog + query execution as MCP tools so external agents (Claude, etc.) can discover datasets and run governed queries — this mirrors Dremio's own "Dremio MCP Server" and agent feature.
- Stack: `quarkus-langchain4j` (reactive LLM orchestration), a vector store (pgvector on Postgres to start; swappable for a dedicated vector DB later).

### 5.6 API Gateway / BFF
- Single reactive entry point (`quarkus-rest-reactive` + `quarkus-reactive-routes`) that fronts the UI and external clients, does authN/authZ via Identity Service tokens, and fans out to Dremio OSS REST/Flight SQL plus the new services — lets the existing Dremio UI keep working largely unmodified while new admin screens (governance, catalog, AI) are added as new UI modules behind the same gateway.

---

## 6. Cross-Cutting Data & Messaging Architecture

- **Event bus**: Kafka (or Redpanda for lighter footprint) as the backbone. Dremio job/query lifecycle events, catalog change events, and access events are published via a **Debezium-style outbox** or direct instrumentation hooks in the adapter layer.
- **Per-service datastore**: each microservice owns its own Postgres schema/database (no shared DB) — consistent with your existing preference for clean separation of concerns; Dremio's own KVStore is left untouched.
- **Synchronous calls**: gRPC (via `quarkus-grpc`, Mutiny-based reactive stubs) for latency-sensitive paths Dremio's coordinator calls inline (auth check, masking policy lookup). REST for admin/UI-facing APIs.
- **Schema contracts**: Protobuf for gRPC, OpenAPI (Iceberg REST spec compliance for Catalog Service) for REST.

---

## 7. Integration Points with Dremio OSS Core (no fork required)

| Dremio SPI / Extension Point | Used By | Mechanism |
|---|---|---|
| `AccessControlChecker` / custom `AccessDelegate` | Identity, Governance | Thin adapter jar registered via Dremio's plugin mechanism; delegates to gRPC calls |
| `StoragePluginConfig` (Iceberg REST catalog client — already ships in OSS) | Catalog Service | Point Dremio at the new Catalog Service's REST endpoint like any Iceberg REST catalog |
| Job/Reflection event listeners (`services/jobs`, `services/accelerator`) | Governance (audit/lineage), Workload Orchestrator | Listener publishes events to Kafka |
| Coordinator REST API (`dac/backend`, JAX-RS) | API Gateway | Reverse-proxied/extended, not modified |
| Executor provisioning (`provision` module, K8s) | Workload Orchestrator | Orchestrator manages executor pod scaling directly via K8s API, coordinating with Dremio's provisioning config |

This adapter-jar approach means the plan does **not** require maintaining a long-lived fork of `dremio-oss` core execution code — only small, mergeable adapter modules plus configuration.

---

## 8. Technology Stack

| Concern | Choice |
|---|---|
| Service framework | Quarkus (reactive, CDI, native-image capable) |
| Reactive programming model | Mutiny (`Uni`/`Multi`) |
| REST | RESTEasy Reactive |
| RPC | gRPC via `quarkus-grpc` |
| Persistence | Hibernate Reactive with Panache + Postgres (per-service DB); Flyway for migrations |
| Messaging | SmallRye Reactive Messaging + Kafka |
| Identity | Keycloak (IdP broker) + `quarkus-oidc` |
| Vector store | pgvector (Postgres extension) initially |
| LLM orchestration | `quarkus-langchain4j` |
| Observability | Micrometer + OpenTelemetry + Prometheus/Grafana |
| Testing | `quarkus-junit5`, Testcontainers (Postgres, Kafka, Keycloak) |
| Deployment | Kubernetes, Helm charts per service, Kustomize overlays per environment |

This matches your established pattern from the AI coding agent backend (Reactive Quarkus, CDI-discovered pluggable strategies, Flyway migrations, Micrometer instrumentation, SSE where needed) — the same conventions should carry over here for consistency.

---

## 9. Security Architecture Detail

- **AuthN**: OIDC/SAML federation at the gateway; Dremio coordinator trusts JWTs signed by the Identity Service (JWKS rotation).
- **AuthZ**: RBAC with custom roles stored in Identity Service; policy decisions cached at the gateway/adapter with short TTL to avoid latency on the hot query path.
- **Row/Column masking**: policy definitions in Governance Service; enforced by rewriting the logical plan (Calcite `RelNode` view wrapping) before Dremio's optimizer runs — implemented in the adapter layer, not the coordinator itself.
- **Audit**: append-only event log, exported to SIEM-compatible sinks for compliance evidence.
- **Secrets/keys**: intracluster encryption via mutual TLS between services (Quarkus supports this natively) and a dedicated KMS integration (Vault or cloud KMS) for custom encryption keys.

---

## 10. Deployment Architecture

- Kubernetes-native throughout, mirroring Dremio's own control-plane/execution-plane split conceptually:
  - **Control plane namespace**: Identity, Governance, Catalog, Workload Orchestrator, Semantic AI, Gateway — each a Deployment + HPA.
  - **Execution plane**: Dremio coordinator (StatefulSet, 1-3 replicas for HA) + executor pools (managed dynamically by Workload Orchestrator).
  - Shared: Postgres (per-service schema, or managed cloud Postgres), Kafka cluster, Keycloak, object storage.
- Helm umbrella chart with one sub-chart per microservice; environment-specific values files for dev/staging/prod.
- This also directly delivers two OSS gaps for free: **High Availability** (multi-replica coordinator + orchestrated executors) and **out-of-the-box monitoring** (Prometheus/Grafana dashboards shipped with the Helm charts).

---

## 11. Phased Roadmap

| Phase | Scope | Outcome |
|---|---|---|
| **0 — Foundation** | Repo scaffolding, shared Quarkus BOM/parent POM, CI/CD, Helm skeleton, event bus + observability stack, adapter-jar skeleton against `dremio-oss` SPIs | Empty but deployable platform alongside a vanilla Dremio OSS cluster |
| **1 — Identity & Access** | OIDC/LDAP federation, RBAC, custom roles, PATs, service users | Closes IAM gap; unlocks everything downstream that needs authz |
| **2 — Governance** | Audit logging, lineage graph, row/column masking | Closes Governance gap |
| **3 — Catalog Service** | Iceberg REST catalog, time travel, auto-optimize/clustering | Closes Open Catalog gap |
| **4 — Workload Orchestration** | Elastic engines, auto-start/stop, WLM, monitoring | Closes Platform gap |
| **5 — Semantic AI Layer** | Embeddings/search, AI wikis/labels, AI functions, MCP server, agent | Closes AI Semantic Layer gap |
| **6 — Hardening** | HA validation, security certs readiness (SOC2/HIPAA/GDPR evidence via Governance Service), load testing, zero-downtime upgrade path | Production-readiness |

Each phase is itself decomposable into the module-by-module, continuation-prompt-friendly build style (domain model → repository layer → service layer → REST/gRPC layer → tests) used for prior builds like the AI coding agent backend — recommended so each service can be built incrementally to production quality rather than scaffolded and left thin.

---

## 12. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Deep coupling temptation (patching `dremio-oss` core instead of using adapters) | Enforce adapter-jar boundary in code review; core changes require explicit justification |
| Row/column masking correctness (plan-rewrite bugs can leak data) | Extensive Calcite plan-rewrite test suite; fail-closed default (deny/mask on policy-service unavailability) |
| Event bus becomes a bottleneck for audit-critical paths | Synchronous gRPC for anything correctness-critical (masking, authz); Kafka only for eventually-consistent concerns (lineage, audit trail, AI indexing) |
| License/version drift against upstream `dremio-oss` | Pin to a specific OSS release tag per platform version; track upstream SPI changes via a compatibility test suite |
| Scope creep re-implementing the query engine | Explicit non-goal, stated up front (Section 1) and revisited at each phase gate |

---

## 13. Suggested Next Step

Before writing code: produce a **detailed design doc for Phase 0 + Phase 1** (Foundation + Identity & Access) — API contracts (OpenAPI/protobuf), data model (ERD), and the adapter-jar interface against `dremio-oss`'s `AccessControlChecker`. That's the natural first implementation increment once this architecture is agreed on.
