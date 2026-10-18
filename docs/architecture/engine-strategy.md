# Toba engine strategy

## Product boundary

Toba is the independent lakehouse control plane. Andalus BI is an independent analytics product and integrates through stable Toba APIs. Neither product is required to be embedded in the other.

Toba owns identity, authorization, policy decisions, audit, catalog/metadata, workload admission and routing, query-job ownership, agent tools, and operational control. Execution engines own physical query planning/execution and provider-specific capabilities.

## First backend and long-term model

Dremio OSS is the first backend. Toba should not be defined as a Dremio wrapper, and engine-specific concepts must not leak into public Toba contracts where a portable concept exists.

The new `engine-spi` module is a dependency-free Java 17 contract so it can be used by modern Toba services and by adapters constrained by the backend's JVM baseline. Engine-specific implementations are separate modules. Capability support is explicit; the control plane must never assume that all engines support cancellation, parameter binding, profiles, transactions, or time travel.

Initial adapter roadmap:
1. Define stable engine contracts and capability negotiation.
2. Implement and test Dremio submission/status/results/cancel against a pinned Dremio OSS version.
3. Add workload routing and job lifecycle around the engine contract.
4. Add a second backend only after conformance tests establish portability.
5. Keep provider extensions behind adapters; upstream engine forks are a last resort.

## Security boundary

The SPI's `EngineQuery` is a trusted internal request, not a public API and not an authorization mechanism. Before `submit`, Toba must authenticate the caller, resolve tenant/workspace context, authorize source/table/column/metric access, apply row/column policies, validate the SQL/semantic plan, enforce resource limits, and record an audit event.

Every status, profile, result, and cancellation request must resolve a Toba-owned job record and re-check its owner/tenant and current policy. A provider job ID is never proof of ownership. Secrets must be obtained from a credential provider and never stored in query tags, SQL, result payloads, or logs.

If users can reach an engine directly, control-plane checks alone are not sufficient to guarantee enforcement. Production deployment must either prevent bypass paths or implement and test enforcement at the engine/source boundary.

## API stability

- Keep SPI dependency-free and Java 17 compatible.
- Add capabilities rather than infer features from product names.
- Use Toba-owned query IDs separately from provider job IDs.
- Use bounded pages, timeouts, row limits, sanitized diagnostics and explicit unsupported-operation failures.
- Treat changes to public record fields or state meanings as versioned contract changes.
- Every engine implementation must pass the same contract test suite plus provider-specific integration tests.
