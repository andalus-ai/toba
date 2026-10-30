# Toba Architecture Blueprints

> Target architecture. Toba is an independent, engine-agnostic, governed lakehouse control plane. Farabi is a client.

## Logical architecture

```mermaid
flowchart TB
  Farabi["Farabi BI Client"] --> API["Toba APIs / SDK"]
  Other["Other Clients"] --> API
  API --> Identity["Identity / Tenant Policy"]
  API --> Catalog["Catalog and Metadata"]
  API --> Planner["Workload / Query Orchestration"]
  Planner --> Governance["Governance and Resource Limits"]
  Governance --> EnginePort["Execution Engine Port"]
  EnginePort --> Dremio["Dremio Adapter"]
  EnginePort --> OtherEngine["Other Engine Adapters"]
  Catalog --> Metadata["Metadata Store"]
  Planner --> Jobs["Durable Jobs / Audit"]
```

## Query execution sequence

```mermaid
sequenceDiagram
  participant Client
  participant API as Toba API
  participant Policy as Policy Engine
  participant Planner as Orchestrator
  participant Engine as Engine Adapter
  Client->>API: Query with authenticated context
  API->>Policy: Check tenant, resource and dataset access
  alt Denied
    Policy-->>Client: Denied
  else Authorized
    Policy-->>Planner: Constraints and permitted scope
    Planner->>Engine: Submit bounded query
    Engine-->>Planner: Job identifier
    loop Until terminal state
      Planner->>Engine: Poll status
      Engine-->>Planner: Status
    end
    Planner-->>Client: Authorized result metadata
  end
```

## Job lifecycle

```mermaid
stateDiagram-v2
  [*] --> Submitted
  Submitted --> Authorized
  Submitted --> Denied
  Authorized --> Queued
  Queued --> Running
  Running --> Succeeded
  Running --> Failed
  Running --> Cancelled
  Denied --> [*]
  Succeeded --> [*]
  Failed --> [*]
  Cancelled --> [*]
```

## Architectural rules

- No mandatory dependency on Farabi or any single execution engine.
- Tenant isolation, data policies, audit and quotas apply before engine dispatch.
- Support retries, cancellation, idempotency and durable job reconciliation.
- Engine adapters translate execution protocols without leaking vendor types into core contracts.
- Integrate identity and tenancy modules through explicit contracts where appropriate.
