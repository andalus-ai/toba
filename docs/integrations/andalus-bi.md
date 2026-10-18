# Andalus BI ↔ Toba integration contract (draft)

## Architecture decision

Toba is an independently deployed Dremio OSS companion platform, **not** a generic JDBC wrapper. Its `dremio-adapter` is a plugin JAR loaded inside the Dremio coordinator, while identity, governance, catalog, workload, and semantic-AI capabilities are separate Quarkus services.

Andalus BI remains the owner of dashboard artifacts, semantic metric definitions, user query requests, chart rendering, and agent workflows. Toba remains the owner of Dremio-adjacent access control, catalog, governance, workload orchestration, and its existing semantic-AI discovery functions. Avoid copying either platform's domain model into the other.

## Integration boundaries

| Operation | Andalus BI | Toba | Required prerequisite |
| --- | --- | --- | --- |
| Dataset search | Calls governed discovery adapter | `semantic-ai-service` MCP search tools | JWT + per-dataset authorization |
| Catalog metadata | Maps source metadata to BI semantic catalog | `catalog-service` Iceberg REST subset | Authenticated gateway, schema normalization |
| Query execution | Compiles/validates actor-authorized semantic plans | Dremio coordinator query endpoint (not yet implemented in this repo) | Confirm actual Dremio SQL/Flight API, auth and parameterization |
| Authorization | Propagates principal and workspace context | `identity-access-service` AuthCheckService | Verified identity mapping and fail-closed checks |
| Governance | Applies BI policy, reconciles source policy | `governance-lineage-service` | Enforce source-side row/column restrictions |
| Workload | Requests bounded execution | `workload-orchestrator-service` | Real coordinator queue metrics and quota enforcement |

## Minimal proposed query adapter

This is a **proposed contract**, not an implemented Toba API:

```java
public interface TobaQueryGateway {
    QueryHandle submit(AuthorizedQuery query);
    QueryStatus status(QueryHandle handle);
    Page<QueryRow> fetch(QueryHandle handle, String pageToken, int pageSize);
    void cancel(QueryHandle handle);
}
```

`AuthorizedQuery` must include actor identity, tenant/workspace scope, a server-validated semantic query plan, allowed dataset identifiers, deadline, row/byte budgets, and an idempotency key. It must **not** accept arbitrary SQL from an untrusted agent or client. The gateway must translate this into the real Dremio query protocol, whose exact API/version and authentication requirements need to be confirmed.

## Security requirements

1. All BI-facing REST routes must authenticate the caller. The current Toba README explicitly reports that its REST endpoints are generally unauthenticated; do not expose them directly to BI users.
2. Re-check authorization at execution time, not just during catalog discovery. Do not treat workspace membership as row-level authorization.
3. Propagate a verified principal (or securely exchanged service identity with delegated claims); never trust caller-supplied user IDs.
4. Deny execution when policy, identity, or masking lookups fail.
5. Ensure tenant isolation for query handles, pagination tokens, result caching, and cancellation.
6. Restrict execution to read-only workloads with quotas, deadlines, audit records, and cancellation support.
7. Never log credentials, raw sensitive query results, or unrestricted SQL parameters.

## Implementation sequence

- **T0:** confirm BI branch/build baseline and Dremio endpoint/version. Produce an explicit source-to-semantic schema mapping.
- **T1:** implement authenticated metadata discovery via a Toba client, with authorization tests.
- **T2:** implement a Dremio query transport (REST or Arrow Flight SQL only after API verification), query state, pagination, timeout and cancellation.
- **T3:** implement BI `SourceConnector` adapter that uses Toba/Dremio transport, with actor-aware validated plans.
- **T4:** run end-to-end tests for denied datasets, cross-workspace access, masking, expired credentials, query cancellation, and streaming/paged results.

## Known gaps in Toba at time of review

Based on the repository's README and module READMEs:
- Most REST endpoints are unauthenticated.
- `dremio-adapter` does not yet implement real Dremio `StoragePlugin` or `PluggableFunctionRegistry` integrations.
- Semantic MCP supports dataset discovery, not governed query execution.
- Workload orchestration lacks live coordinator queue-depth metrics.
- Iceberg catalog commits and several production-hardening capabilities remain incomplete.

This document is an integration specification only; it does not claim these gaps have been closed.
