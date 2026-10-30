# Control plane and data plane — target

```mermaid
flowchart TB
  Clients["API / SDK / Farabi / Agents"] --> Gateway["Authenticated Boundary"]
  subgraph Control["Toba Control Plane"]
    Gateway --> Governance["Identity / Membership / Policy"]
    Gateway --> Catalog["Catalog / Metadata"]
    Gateway --> Workload["Workload Admission / Routing"]
    Gateway --> Jobs["Durable Query Job Lifecycle"]
    Gateway --> AgentOps["Governed Agent Tools"]
    AgentOps --> Governance
    Workload --> Governance
    Jobs --> Audit["Audit / Lineage / Telemetry"]
  end
  subgraph Data["Data Plane"]
    Dremio["Dremio Engine"]
    Future["Other Engines"]
    Storage[("Object Storage / Tables")]
    Dremio --> Storage
    Future --> Storage
  end
  Workload --> Dremio
  Workload -.-> Future
  Jobs --> Dremio
  Catalog --> Storage
```

**Trust rule:** Direct client access to engines must be blocked or equivalently policy-enforced at the engine/source boundary. **Status:** logical target, not deployed topology.
