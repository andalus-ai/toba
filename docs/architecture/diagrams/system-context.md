# System context — target

```mermaid
flowchart LR
  Analyst["Analysts / Data Engineers"] --> API["Toba Governed API"]
  Operator["Platform Operators"] --> API
  Agent["Trusted Agent Clients"] --> API
  Farabi["Farabi BI — Independent Product"] --> API
  API --> Core["Toba Control Plane"]
  Core -.-> Numu["Numu / Identity Provider"]
  Core --> Dremio["Dremio OSS Adapter"]
  Core -.-> Other["Future Engine Adapters"]
  Core --> Catalog["Catalog and Metadata Providers"]
  Dremio --> Lake[("Lakehouse Storage")]
  Other -.-> Lake
  Catalog --> Lake
```

**Ownership:** Toba owns policy decisions and query lifecycle. Farabi owns BI/dashboard experiences; Numu supplies trusted identity evidence when configured. Engines execute physical queries. **Status:** aspirational context; adapter availability must be confirmed independently.
