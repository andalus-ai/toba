# Toba Lakehouse Platform

**Toba is an independent, engine-agnostic, agentic lakehouse control plane.** It owns identity, authorization, governance, catalog services, workload orchestration, audit, semantic data intelligence and agent tools. Dremio OSS is the first execution backend—not Toba's product identity or a permanent engine lock-in.

Toba and Andalus BI remain separate products. Andalus BI owns dashboard authoring, visualization and AI-assisted analytics; it integrates with Toba through stable platform APIs. Other applications can consume Toba without deploying BI.

Architecture and roadmap: `toba-extension-plan.md`. The engine contract is documented in `docs/architecture/engine-strategy.md`.

## Repo layout

```
toba-lakehouse-platform/
├── pom.xml                          root aggregator + shared build config
├── engine-spi/                     dependency-free Java 17 multi-engine execution contract
├── platform-bom/                    internal BOM: shared contract/library versions
├── platform-common/                 shared RFC 7807 error model, base exceptions
├── identity-access-service/         Phase 1 — RBAC, JWT issuance, PATs, service accounts
├── governance-lineage-service/      Phase 2 — masking policies, lineage graph, audit trail
├── catalog-service/                 Phase 3 — Iceberg REST Catalog implementation
├── dremio-adapter/                  the thin jar that wires Dremio OSS to everything above
├── workload-orchestrator-service/   Phase 4 — elastic engines, WLM routing, K8s scaling
├── semantic-ai-service/             Phase 5 — embeddings, wiki suggestions, AI functions, MCP tools
│   └── README.md (each module)      module-level status + design notes + known gaps
```

API Gateway (plan Section 5.6) is intentionally not part of this repo — that role is
filled by [bhangun/iket](https://github.com/bhangun/iket) instead.

## Status snapshot

| Component | Status | Important limitation |
|---|---|---|
| engine-spi | Initial Java 17 contracts and unit tests added in current PR | Dremio adapter implementation and conformance suite still pending |
| identity-access-service | Initial identity/RBAC/PAT service exists | README records JWT issuance/principal resolution and real request identity as incomplete |
| governance-lineage-service | Policy definitions, lineage and identity-event audit ingestion exist | Masking enforcement hook and tests remain incomplete |
| catalog-service | Core Iceberg REST subset and real TableMetadata serialization exist | Commit transactions, snapshots/time travel, partition/sort evolution remain incomplete |
| dremio-adapter | Client and SPI-mirror code exists | Real Dremio-side plugin implementation and version-pinned integration are not complete |
| workload-orchestrator-service | Engine records, K8s scaling controls and schedulers exist | Real Dremio queue-depth telemetry and job-submission hook are missing |
| semantic-ai-service | AI/MCP implementation exists | README says MCP is secured but REST endpoints remain unauthenticated; real catalog enrichment and integration tests remain |

Treat these modules as an active engineering prototype, not as a production-ready enterprise distribution. The module READMEs and `docs/architecture/current-gaps.md` record the known gaps.

## Build order

Each service is built in the same layer order, one continuation prompt at a time,
to make each increment testable; production readiness is only claimed after its security and integration gates pass:

`domain model → repository layer → service layer → REST/gRPC layer → tests`

## Local build

```bash
mvn -pl engine-spi,platform-bom,platform-common,identity-access-service,governance-lineage-service,catalog-service,dremio-adapter,workload-orchestrator-service,semantic-ai-service -am verify
```

Run any service individually:

```bash
mvn -pl identity-access-service -am quarkus:dev
mvn -pl governance-lineage-service -am quarkus:dev
mvn -pl catalog-service -am quarkus:dev
mvn -pl workload-orchestrator-service -am quarkus:dev
mvn -pl semantic-ai-service -am quarkus:dev
```

`dremio-adapter` is not a Quarkus app and has no `quarkus:dev` target — it's a plain
jar meant to run inside a real Dremio OSS coordinator process. See its README for
what running its tests looks like (`mvn -pl dremio-adapter -am test`, no live
services needed — the gRPC client is mocked) and for exactly how it would be wired
into a real Dremio deployment once you have one to reconcile its SPI mirrors against.

Each service owns its own Postgres database (`identity`, `governance`, `catalog`)
and, where relevant, its own Kafka topics (`identity.events`, `catalog.events`) — no
shared database, per the plan's cross-cutting data architecture (Section 6). Quarkus
Dev Services spins up throwaway Postgres/Kafka containers automatically in
`quarkus:dev` and `test` — no manual setup needed to run any service or its tests
locally.

## Prerequisites

- JDK 21 (JDK 17 minimum for `dremio-adapter`, which targets a lower release to stay
  compatible with a wider range of dremio-oss JVM requirements — see its `pom.xml`)
- Maven 3.9+
- Docker (for Quarkus Dev Services)

## Suggested next steps

- **`identity-access-service`**: OIDC/LDAP federated login flow (local login + JWT
  issuance is done; federated login isn't yet).
- **`governance-lineage-service`**: gRPC masking-policy lookup (currently REST-only).
- **`catalog-service`**: verify `S3ObjectStorageClient` against a real MinIO
  instance (Testcontainers) — the one thing its "checked the API shape"
  verification couldn't cover without a reachable endpoint; partition spec / sort
  order support in `CreateTableRequest`;
  a commit-transaction endpoint so table metadata can actually evolve after creation.
- **`dremio-adapter`**: implement the actual `StoragePlugin` decorator and
  `PluggableFunctionRegistry` against a real local `dremio-oss` checkout (both
  interfaces are now identified precisely — see `RECONCILIATION_NOTES.md` — this
  is "go build it" work now, not "go find it"); also still needs unit tests for
  `MaskingPolicyHttpClient`'s response parser (`AiFunctionHttpClient`'s parser
  already has them, as a template).
- **`workload-orchestrator-service`**: replica-count drift correction (K8s reports
  a different ready-replica count than the DB believes — nothing reconciles this
  yet); real queue-depth data (still fed by manual REST calls, not an actual
  Dremio job-submission hook).
- **`semantic-ai-service`**: an MCP integration test against the real `/mcp`
  endpoint (current authz tests are unit-level, mocked gRPC client); richer
  `CatalogEventConsumer` embeddings (pulling real schema from catalog-service
  instead of just the namespace/name path).
- **Cross-cutting**: Section 11 Phase 6 (HA validation, compliance evidence, load
  testing) is the remaining hardening work once the above gaps close.
