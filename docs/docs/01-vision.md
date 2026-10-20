# 01 — Vision and product strategy

## Mission

Make governed data exploration, analytical application building and reusable dashboard publishing available through one extensible platform. Farabi should work as a standalone BI server, a library embedded in an Andalus application, or an analytics capability invoked by trusted agents.

## Product pillars

1. **Conventional BI:** semantic datasets, metrics, filters, chart specifications, dashboard layout, saved artifacts, permissions, sharing and refresh.
2. **Agentic BI:** natural-language requests translated into explainable, policy-checked analytical plans and interactive results.
3. **Dynamic → static promotion:** agent-created visualizations can become versioned, reproducible, reviewable dashboard artifacts; they do not remain ephemeral chat output.
4. **Composable integrations:** Toba is the primary governed data integration; Dremio is an execution provider behind Toba/adapter boundaries. Tenun supplies chart rendering and Batik supplies A2UI rendering, with replaceable integration contracts.
5. **Library-first deployment:** the domain and use cases are independent of HTTP, Quarkus, databases, specific AI providers and frontend frameworks.
6. **Enterprise trust:** tenant/workspace isolation, identity-based access, policy enforcement, lineage, auditability, cost controls and safe execution.

## Users and jobs-to-be-done

| Persona | Need | Outcome |
|---|---|---|
| Analyst | Explore datasets, build reports | Reusable governed dashboard |
| Business user | Ask questions in natural language | Explained chart and follow-up exploration |
| Dashboard author | Curate agent-generated findings | Versioned static artifact with approvals |
| Platform engineer | Embed analytics | Java engine API and configurable adapters |
| Data steward | Control definitions and access | Certified metrics, policy and lineage |
| Operator | Run production workloads | Observable, recoverable query lifecycle |

## Product boundaries

**Farabi owns:** analytics use cases, semantic-facing query requests, dashboard/chart specifications, artifact versions, orchestration, policy invocation, publishing and UI intent contracts.  
**Toba owns:** federated lakehouse/data access, catalogs and source connectivity as defined by its own contracts.  
**Tenun owns:** chart rendering and chart-specific frontend behavior.  
**Batik owns:** A2UI presentation/rendering framework.  
**AI providers own:** inference; they are never the final authority for authorization or query execution.

## Success measures (proposed)

- Embedded engine and Quarkus runtime pass the same contract tests.
- All query submissions pass authenticated authorization and governed semantic compilation.
- A user can ask a question, inspect a chart, promote it to a static dashboard, and reopen the exact saved version.
- Reconciliation never duplicates remote jobs merely because of an ambiguous timeout.
- Every published artifact records provenance: semantic model version, parameters, source references, author, policy decision and renderer specification version.
- Production readiness is measured by reproducible builds, integration tests, telemetry, recovery and operational runbooks—not by committed source alone.

## Product evolution

**Phase A:** engine contracts, query execution, persistent lifecycle and Quarkus server.  
**Phase B:** conventional BI authoring, visualization specs, static artifacts and governance.  
**Phase C:** AI planner, Batik A2UI interaction and dynamic exploration.  
**Phase D:** dynamic-to-static promotion, collaboration, scheduled delivery and enterprise extensibility.
