# Deployment topology — reference target

```mermaid
flowchart TB
  Users["Clients / Farabi / Agent Runtimes"] --> Edge["TLS / Gateway"]
  subgraph Toba["Toba Deployment"]
    Edge --> API["Quarkus API Instances"]
    API --> IAM["Identity + Policy Adapters"]
    API --> Catalog["Catalog Services"]
    API --> Jobs["Query Job Service"]
    Jobs --> Workers["Fenced Workers / Reconciler"]
    API --> Telemetry["Audit / Metrics / Traces"]
    Workers --> Telemetry
  end
  Jobs --> DB[("Transactional Job Database")]
  Catalog --> Meta[("Metadata Store")]
  Workers --> Dremio["Dremio OSS"]
  Workers -.-> Other["Future Engine"]
  Dremio --> Object[("Lakehouse Object Storage")]
  Other -.-> Object
  IAM -.-> Numu["Numu / Trusted IdP"]
```

**Deployment boundary:** Workers need idempotent recovery; engines must not expose ungoverned direct paths; credentials use secret management. **Status:** conceptual reference, not a claim of deployed HA.
