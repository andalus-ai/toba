# Farabi — Vision, Architecture & Delivery Blueprint

**Status:** Proposed target architecture and roadmap, 2026-10-20.  
**Repository:** `andalus-developer/bi` (modular monorepo).  
**Origin:** Formerly “Andalus BI”; product name **Farabi**.  
**Primary design choice:** Hexagonal, library-first engine; Quarkus is the default runtime, not an engine dependency.

## Document map

1. [Product vision and scope](docs/01-vision.md)
2. [Architecture and module contracts](docs/02-architecture.md)
3. [Domain, query, dashboard and AI workflows](docs/03-workflows.md)
4. [Security, governance and operations](docs/04-governance-operations.md)
5. [Roadmap, milestones and acceptance criteria](docs/05-roadmap.md)
6. [Architecture decisions](docs/adr/README.md)
7. [Mermaid diagrams](docs/diagrams/README.md)

## North star

Farabi is an **embeddable, governed analytics and BI engine** that serves two equally important interaction models: traditional authored dashboards and conversational/agentic exploration. Agent-generated views are reproducible, reviewable and promotable into durable, versioned dashboard artifacts. The same core use cases run in an embedded Java host or in the default Quarkus server.

## Non-goals for the initial release

- Replacing Toba's lakehouse/data virtualization responsibilities.
- Reimplementing Tenun chart rendering or Batik A2UI rendering in the Java engine.
- Letting an LLM submit arbitrary SQL or bypass data governance.
- Supporting all possible runtimes on day one; maintain runtime portability by contract, prove it with embedded Java and Quarkus.

## Current status versus target

Earlier implementation work was reported as draft PRs **#25–#30** in `andalus-developer/bi`: initial Maven reactor, engine contracts, Dremio and JDBC adapters, Quarkus wiring and durable submission. These changes were **not confirmed as merged, compiled or integration-tested**. They are *foundation slices*, not a completed migration. This blueprint distinguishes those drafts from future target capabilities. GitHub access returned a 403 account-suspended error while preparing these documents, so the files are supplied locally and have **not** been committed to the repository.
