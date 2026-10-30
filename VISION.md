# Toba — Vision, Mission and Product Charter

**Status:** Normative product direction. This document is not an implementation-completeness claim.

## Vision

Toba is an independent, open, enterprise-grade, AI-native and agentic lakehouse platform. It unifies data access, metadata, governance, query execution, workload management and optimization across interchangeable engines and data environments.

**Toba is not a Dremio wrapper with AI features.** Dremio OSS is the first execution backend; it must not define Toba's public model or limit future providers.

## Mission

Enable organizations and trusted agents to discover, govern, query, operate and optimize distributed lakehouse data through one coherent, extensible and auditable control plane, without mandatory dependence on a single engine, storage provider, AI model or analytics application.

## Product pillars

1. **Independent lakehouse:** Toba owns its APIs, domain, query-job lifecycle, metadata and operational control.
2. **Multi-engine execution fabric:** engines are capability-declared adapters; Dremio first, others after contract conformance.
3. **Open data and metadata:** interoperable catalogs, Iceberg-compatible table operations, storage adapters, discoverability and provenance.
4. **Unified governance:** trusted identity, tenant/workspace isolation, policy enforcement, audit, lineage and data controls across every execution path.
5. **Agentic intelligence:** governed tools for discovery, planning, diagnostics, operations and optimization; agent proposals never bypass policy.
6. **Intelligent operations:** workload admission, routing, observability, cost/resource controls and safe optimization.
7. **Composable ecosystem:** Farabi is an independent BI consumer; Numu is an optional reusable identity/membership integration, not a required core dependency.
8. **Verifiable enterprise quality:** correctness, resilience, upgradeability, interoperability and measured performance.

## Product ownership boundaries

| Toba owns | Other systems own |
|---|---|
| Lakehouse APIs, catalog orchestration, query-job identity and workload routing | Engines own physical execution and engine-specific optimization |
| Authoritative Toba policy decisions and enforcement coordination | Numu may provide verified identity and membership evidence through an adapter |
| Governed agent tools and operation approvals | AI providers produce suggestions/inference, never final authorization |
| Data-platform integration contracts | Farabi owns semantic BI, charts, dashboards and dynamic-to-static publishing |

Toba must operate without Farabi. Its core must not require Numu or Dremio-specific classes.

## Success outcomes

- One governed query API works with a Dremio adapter and is portable to a second engine through conformance tests.
- A catalog client can discover metadata and perform permitted operations while cross-tenant access is denied.
- Every job has a Toba-owned identifier, scope, audit trail, bounded resource limits and enforceable lifecycle.
- An agent can inspect metadata and propose a query; execution requires the same controls as a human request.
- The system can detect, diagnose and safely recover from engine failures and ambiguous submission outcomes.
- Embedded/domain contracts can be tested without Quarkus, Dremio, Farabi or a live AI provider.
- Production readiness is demonstrated by reproducible builds, integration tests, operational runbooks and threat-model review.

## Non-goals

- Rebuilding a general-purpose identity provider instead of integrating Numu or equivalent providers.
- Making Dremio OSS source code the only possible execution architecture.
- Duplicating Farabi dashboard or chart-authoring functionality.
- Allowing agents to execute unrestricted SQL or destructive operations without governed approval.
- Claiming readiness based solely on merged or committed source.

## Decision rule

When a proposal improves one adapter but compromises engine neutrality, tenant isolation, agent safety or core independence, reject or redesign it. Exceptions require a documented architecture decision record (ADR), tests and an explicit migration path.
