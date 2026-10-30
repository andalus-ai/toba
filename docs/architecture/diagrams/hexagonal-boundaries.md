# Hexagonal dependency boundaries — target

```mermaid
flowchart LR
  subgraph Inputs["Inbound adapters"]
    REST["REST / Quarkus"]
    SDK["Embedded SDK"]
    Tools["Agent Tools"]
  end
  subgraph Inner["Engine-neutral Toba core"]
    UseCases["Application Use Cases"]
    Domain["Domain Invariants"]
    Ports["Ports / SPI"]
    UseCases --> Domain
    UseCases --> Ports
  end
  subgraph Outputs["Outbound adapters"]
    Engine["Dremio / Other Engines"]
    Identity["Numu / OIDC"]
    Meta["Catalog / Storage"]
    Event["Persistence / Audit"]
  end
  REST --> UseCases
  SDK --> UseCases
  Tools --> UseCases
  Engine -.-> Ports
  Identity -.-> Ports
  Meta -.-> Ports
  Event -.-> Ports
```

**Dependency rule:** Core depends on port abstractions; concrete adapters implement them and are wired by a composition root. Dotted arrows represent implementation-of-port relationships, **not** core imports of adapters. **Status:** desired modularity; enforce with dependency checks.
