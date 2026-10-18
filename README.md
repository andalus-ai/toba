# Dremio Reactive Platform

Enterprise-capability layer for [`dremio-oss`](https://github.com/dremio/dremio-oss),
built as independently-deployable Quarkus reactive microservices sitting alongside an
unmodified Dremio OSS cluster (strangler-fig, not a fork). Full architecture rationale,
feature-gap analysis, and phased roadmap: see `dremio-reactive-extension-plan.md`.

## Repo layout

```
dremio-reactive-platform/
├── pom.xml                          root aggregator + shared build config
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

| Service | Domain | Repo | Service | REST | gRPC/Events | Tests |
|---|---|---|---|---|---|---|
| identity-access-service | ✅ | ✅ | ✅ (incl. JWT issuance) | ✅ | ✅ gRPC AuthCheckService | 🟡 started |
| governance-lineage-service | ✅ | ✅ | ✅ | ✅ | ✅ Kafka audit consumer | not started |
| catalog-service | ✅ | ✅ | ✅ (real Iceberg TableMetadata + real/opt-in S3 backend) | ✅ (core Iceberg REST subset) | ✅ Kafka publisher | 🟡 started |
| dremio-adapter | n/a (plugin jar, not a service) | n/a | n/a | n/a | ✅ gRPC + HTTP clients wired | 🟡 started |
| workload-orchestrator-service | ✅ | ✅ | ✅ | ✅ | ✅ Kafka publisher + K8s client + idle-stop + queue-depth scale-up schedulers | 🟡 started |
| semantic-ai-service | ✅ | ✅ | ✅ | ✅ | ✅ Kafka consumer + authenticated/authorized MCP server | 🟡 started |

All five planned phases (Section 11) now have a running implementation. Each
module's README has the precise per-layer status table and a "Known gaps" section —
read that before assuming a module is spec-complete or production-ready. Worth
flagging at the root level:

- **catalog-service** now writes and reads REAL Apache Iceberg `TableMetadata` —
  every API used (`SchemaParser`, `TableMetadata.newTableMetadata`,
  `TableMetadataParser`) was verified against Iceberg 1.11.0's actual source
  before use, same research technique as the `dremio-oss` reconciliation. Its
  object-storage backend now has a real, opt-in S3-compatible option too
  (`catalog.storage.backend=s3`, plain AWS SDK v2) alongside the local-disk
  default — but that S3 client's API calls were checked against AWS's docs, not
  tested against a live bucket (no S3-compatible endpoint was reachable in this
  environment). Still simplified: always unpartitioned/unsorted, no
  commit-transaction/snapshot endpoint — see `catalog-service/README.md`.
- **dremio-adapter**'s `spi/` interfaces are now **partially verified** against
  real `dremio-oss` source (a shallow `git clone --filter=blob:none` was used to
  read actual interface signatures — see `dremio-adapter/RECONCILIATION_NOTES.md`).
  Three findings so far: (1) the real access-control hook is
  `StoragePlugin.hasAccessPermission` (per-source, impersonation-gated); (2) row/
  column masking is confirmed edition-gated at the handler level in OSS, making the
  plan's adapter-side Calcite-rewrite the only viable approach there; (3) custom
  functions (for `AI_SUMMARIZE`/`AI_CLASSIFY`) DO have a genuine classpath-scanned
  plugin mechanism (`PluggableFunctionRegistry`), but its real per-row execution
  contract is synchronous/blocking with no batching hook — a real, tested HTTP
  client with response caching exists (`ai/AiFunctionHttpClient`), but the actual
  Dremio-side function implementation isn't built, and shouldn't be used for
  batch-scale queries even once it is. Still unverified: the actual plugin
  registration mechanism for `StoragePlugin`; none of the three real Dremio-side
  classes are implemented yet (all three are "go build it" work now, not "go find
  it" research).
- **workload-orchestrator-service** now implements both halves of auto-scaling —
  idle-timeout scale-down (V1) and queue-depth-triggered scale-up (`ScaleUpScheduler`,
  tested end-to-end via `ScaleUpFlowTest`). Its K8s client still defaults to a safe
  no-op until explicitly enabled. What's still missing: real queue-depth data —
  nothing yet reports genuine queue depth from an actual Dremio coordinator, same
  gap `WlmQueueRuleService.resolveEngineFor` has been waiting on since V1.
- **semantic-ai-service**: chat completion is real (Anthropic). Embeddings have a
  real, opt-in option too (OpenAI's `text-embedding-3-small`, via
  `ai.embedding.provider=openai` + `OPENAI_API_KEY`) — a deliberately different
  vendor from chat, since Anthropic has no embeddings API. The MCP tool surface is
  a **real, working, authenticated, and per-tool authorized MCP server**
  (`quarkus-mcp-server-http` at `/mcp`, secured with the same platform JWTs
  identity-access-service issues, gated per-call via a gRPC call to that same
  service's real `AuthCheckService` — the same contract `dremio-adapter` calls).
  The REST API remains unauthenticated, matching every other service in this repo.
- **API Gateway**: intentionally not built — the person building this platform is
  using [bhangun/iket](https://github.com/bhangun/iket) for that role instead.

## Build order

Each service is built in the same layer order, one continuation prompt at a time,
so every increment is production-quality rather than scaffolded-and-abandoned:

`domain model → repository layer → service layer → REST/gRPC layer → tests`

## Local build

```bash
mvn -pl platform-bom,platform-common,identity-access-service,governance-lineage-service,catalog-service,dremio-adapter,workload-orchestrator-service,semantic-ai-service -am install
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
