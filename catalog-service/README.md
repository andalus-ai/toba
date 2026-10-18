# Catalog Service

Phase 3 of the roadmap in `dremio-reactive-extension-plan.md`. Implements the
[Apache Iceberg REST Catalog spec](https://iceberg.apache.org/spec/#rest-catalog) —
the highest-leverage service in the whole plan, since Dremio OSS already ships a
client (`IcebergRestCatalog` `StoragePlugin`) that can attach to any spec-compliant
catalog with zero core changes. Point Dremio at this service the same way you'd
point it at any external Iceberg REST catalog.

## Status

| # | Layer | Status |
|---|---|---|
| 1 | Domain model + migration | ✅ done |
| 2 | Repository layer | ✅ done |
| 3 | Service layer | ✅ done, backed by REAL Iceberg `TableMetadata` (see below) |
| 4 | REST layer (core spec subset) | ✅ done |
| 5 | Time travel (snapshot list/rollback endpoints) | not started |
| 6 | Auto-optimize/compaction (`RewriteDataFiles`/`ExpireSnapshots` scheduled jobs) | not started |
| 7 | Tests | ✅ started — table creation asserted against real `TableMetadata` JSON content, an invalid-schema rejection test, S3 location-parsing tests, and routing-logic tests for the local/S3 backend selection |

## Endpoints implemented

```
GET    /v1/config
GET    /v1/namespaces?parent={parent}
POST   /v1/namespaces
GET    /v1/namespaces/{namespace}
HEAD   /v1/namespaces/{namespace}
DELETE /v1/namespaces/{namespace}
GET    /v1/namespaces/{namespace}/tables
POST   /v1/namespaces/{namespace}/tables
GET    /v1/namespaces/{namespace}/tables/{table}
DELETE /v1/namespaces/{namespace}/tables/{table}
```

`{namespace}` is the spec's unit-separator-encoded (`\u001F`-joined) multi-level path
segment — see `util/NamespaceCodec` for the encode/decode against this service's
dot-joined internal storage format (`CatalogNamespace.levelsPath`).

## ⚠️ Real Iceberg metadata, verified against the actual library — read this before assuming full spec compliance

`TableService` now builds and persists **real Apache Iceberg `TableMetadata`**, not
a placeholder — and every API used to do it was verified against Iceberg 1.11.0's
actual source (a shallow, blob-filtered `git clone` of `github.com/apache/iceberg`,
the same technique used for the `dremio-oss` reconciliation in `dremio-adapter`)
before any code was written against it:

- **`POST .../tables`** parses the caller's schema with the real
  `org.apache.iceberg.SchemaParser.fromJson(String)`, builds a real `TableMetadata`
  via `TableMetadata.newTableMetadata(Schema, PartitionSpec, String, Map)`,
  serializes it with the real `TableMetadataParser.toJson`, and writes those exact
  bytes through `ObjectStorageClient` to the table's metadata location.
- **`GET .../tables/{table}`** reads those bytes back and re-parses them with the
  real `TableMetadataParser.fromJson(String)` — a genuine round trip through
  Iceberg's own (de)serialization, not a cached DTO. `CatalogFlowTest` asserts the
  response JSON contains real `TableMetadata` markers (`format-version`,
  `table-uuid`) that only a real Iceberg document would have.
- **`CreateTableRequest.schemaJson` is now a real, validated Iceberg schema** — an
  invalid one is rejected with `CATALOG-400-INVALID-SCHEMA` before anything is
  written, not silently stored opaquely (the previous behavior).

**Still simplified — this is not full spec compliance:**

- **Partition spec is always `PartitionSpec.unpartitioned()`** — `CreateTableRequest`
  has no partition-spec field yet.
- **Sort order is always unsorted** — same reason, no field for it yet.
- **No commit-transaction endpoint** (`POST /v1/namespaces/{ns}/tables/{table}` for
  updates — new snapshots, schema evolution). `TableSnapshot` the domain entity
  exists and is migrated, but nothing writes to it yet.
- **Time travel** — snapshot listing/rollback endpoints aren't built, though
  `TableSnapshot`/`TableSnapshotRepository` are ready for them.
- **Auto-optimization/compaction** — no scheduled jobs calling Iceberg's
  `RewriteDataFiles`/`ExpireSnapshots` actions yet (plan Section 5.3).
- **`ObjectStorageClient` now has a real S3-compatible implementation**
  (`S3ObjectStorageClient`, plain AWS SDK v2, opt-in via `catalog.storage.backend=s3`)
  alongside the local-disk default — see "Real object storage" below for exactly
  what's verified vs. not.

## ⚠️ Real object storage — what's verified vs. what isn't

`RoutingObjectStorageClient` picks between two implementations at runtime via
`catalog.storage.backend`:

- **`local`** (default) — `LocalFilesystemObjectStorageClient`, real file I/O
  under `catalog.storage.local-root`. Fully tested (`CatalogFlowTest` exercises it
  end-to-end on every run).
- **`s3`** — `S3ObjectStorageClient`, the plain AWS SDK v2 (not a Quarkus
  extension — see the dependency comment in `pom.xml` for why: the Quarkiverse
  `quarkus-amazon-s3` extension's BOM-version compatibility with this project's
  pinned Quarkus platform version couldn't be verified without a live build in
  this environment, so the plain SDK — independently versioned, no Quarkus
  alignment needed — was used instead). Works against real AWS S3 or any
  S3-compatible endpoint (MinIO, etc.) via `catalog.storage.s3.endpoint-override`.

**Honest caveat on the S3 client specifically:** its AWS SDK v2 API calls
(`S3Client.putObject`/`getObject`/`headObject`) were checked against the SDK's own
current documentation before being written — the same verify-before-build
discipline as the Iceberg and `dremio-oss` reconciliation work — but that is a
weaker claim than "tested against a live bucket." No S3-compatible endpoint was
reachable in this environment to actually exercise it end-to-end.
`S3ObjectStorageClientTest` covers what's testable without one (location parsing,
which happens before any network call); `RoutingObjectStorageClientTest` covers
the routing decision with both backends mocked.

**A real correctness fix landed alongside the S3 addition, not just new code:**
both storage backends do genuinely blocking I/O (disk or network), and
`TableService`'s calls into `ObjectStorageClient` now explicitly run via
`Uni.createFrom().item(...).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`
rather than inline on the reactive event loop. The original local-disk-only version
had this same blocking-I/O-on-the-event-loop issue and it simply hadn't been
caught yet — worth knowing if you're looking for what changed here beyond "added S3."

## Known gaps / next continuation prompts

- **S3 backend is unverified against a live endpoint** — see the "Real object
  storage" section above for the precise caveat (API shape checked against docs,
  never exercised against an actual bucket in this environment).
- **No partition spec or sort order support** in the request DTO — every table is
  created unpartitioned/unsorted regardless of what a real dataset might need.
- **No commit-transaction / snapshot-append endpoint** — a table's metadata never
  changes after creation in this increment.
- **Drop doesn't purge underlying files** — intentional (matches Iceberg's own
  convention that a catalog-level drop isn't necessarily a destructive purge; see
  `TableService.drop`'s comment), but worth knowing if you expected disk space to
  be reclaimed automatically.

## Design notes

- **`catalog_namespace` and `catalog_table` store dot-joined paths**, not a
  self-referencing namespace tree — `NamespaceCodec` handles conversion to/from the
  spec's wire format at the REST boundary only, keeping the storage layer simple.
- **Table properties and namespace properties are stored as JSONB**, deliberately
  schema-flexible since the Iceberg spec allows arbitrary string properties.
- **`catalog.events` Kafka topic is wired** (`CatalogEventPublisher`) publishing
  `TABLE_CREATED`/`TABLE_DROPPED` events, in the same shape identity-access-service
  uses — ready for the Governance Service's `AuditIngestionConsumer` to add a
  consumer for once that's prioritized (see that module's README).

## Running locally

```bash
mvn -pl catalog-service -am quarkus:dev
```

Metadata JSON files are written under `catalog.storage.local-root` (default
`/tmp/dremio-platform-catalog-storage`) — inspect them directly to see real
Iceberg `TableMetadata` documents on disk.

## Next continuation prompt

**"Verify S3ObjectStorageClient against a real MinIO instance"** — spin up MinIO
via Testcontainers in a new integration test, point `catalog.storage.s3.*` config
at it, and confirm the write/read/exists round trip actually works — the one
thing this increment's "checked the API shape" verification couldn't cover
without a reachable S3-compatible endpoint.
