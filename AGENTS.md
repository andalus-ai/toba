# AGENTS.md — Toba Engineering Constitution

**Applies to:** all directories in this repository unless a more specific AGENTS.md adds stricter local instructions. Read this file before planning or editing code.

## 1. North star

Toba is an **independent, open, enterprise-grade, AI-native, agentic, multi-engine lakehouse control plane**. It is **not a Dremio wrapper** and **not Farabi BI**. Optimize for portable governed data access, metadata, execution, operational intelligence and safe autonomy.

Read `VISION.md`, `ARCHITECTURE.md`, `ROADMAP.md` and `docs/architecture/engine-strategy.md` before major design changes. If a task conflicts with these, explain the conflict and propose an ADR; do not quietly redefine the product.

## 2. Hard architectural rules

1. **Core independence:** no Quarkus, Dremio, Numu, Farabi, storage vendor or LLM SDK imports in engine-neutral domain/application contracts.
2. **Ports and adapters:** implement replaceable, capability-declared providers; Dremio OSS is first, not exclusive. Keep Java 17 engine SPI portable.
3. **Toba owns lifecycle:** Toba job IDs, durable state, authorization, workload admission, audit and recovery cannot be delegated implicitly to provider job IDs.
4. **Product boundaries:** Farabi owns BI dashboards/charts; Numu may supply identity/membership via adapters; AI providers only produce untrusted proposals.
5. **Governance everywhere:** enforce tenant/workspace/row/column policies and audit across all query, catalog, results, cancellation and agent paths. Block or secure direct engine bypass.
6. **Trusted actors:** derive canonical identity only from validated credentials and authoritative membership. Never trust `X-Actor-Id`, client-supplied grants, unverified JWT claims or principal strings as actor UUIDs.
7. **Fail closed:** missing/ambiguous authorization providers, unavailable membership, missing grants, invalid credentials and unexpected errors deny access; do not replace failures with empty success.
8. **Agent safety:** typed/allowlisted tools, least privilege, explicit scope, bounded costs, approvals for destructive actions, idempotency, audit and prompt-injection resilience.
9. **No fake completion:** deny-only adapters, stubs and interface declarations are foundations, not functional integrations.
10. **No architecture churn:** preserve compatibility; add ADRs for contract-breaking or cross-boundary decisions.

## 3. Before making changes

- Inspect current `main`, branches, existing modules, tests, documentation and pending work; do not assume prior branches still exist.
- State the user-visible product capability, impacted ports/adapters, threat boundaries and test plan.
- Search for existing implementations before introducing a second query lifecycle, identity resolver, catalog service or authorization source.
- Confirm dependency direction; avoid accidental direct coupling between microservices.
- Use existing Numu and Toba authorization services where appropriate; do not duplicate identity-provider functionality.
- Treat `docs/docs/` with caution: some files currently contain Farabi documentation and are not authoritative Toba requirements.

## 4. During implementation

- Deliver vertical slices: contract + adapter + runtime wiring + deny/allow tests + docs, rather than disconnected placeholder interfaces.
- Keep authentication, membership, grants, actor derivation and policy checks server-side.
- Preserve reactive nonblocking behavior and ensure failures are not swallowed or remapped to misleading success/404 responses.
- Validate query ownership on read, result, profile and cancel operations.
- Prefer explicit capability negotiation and deterministic unsupported-operation errors.
- Add contract and negative security tests for every new engine/identity/catalog/agent boundary.
- Record decisions affecting public APIs, engine neutrality or data safety in an ADR.

## 5. Verification and reporting

Report separately: **committed**, **compiled**, **unit-tested**, **integration-tested**, **runtime-verified**, **production-ready**. Never imply a test ran if it did not. A successful GitHub API commit is not a Maven build.

Minimum checks when environment permits:
- `mvn -pl <affected-modules> -am test` (or documented project-equivalent command).
- Quarkus/CDI startup and dependency-injection validation.
- Authorization tests for allowed, denied, missing provider, provider outage, revoked membership and cross-tenant access.
- Dremio/engine adapter contract tests, persistence/migration tests and negative bypass tests where relevant.
- Lint/static analysis, API compatibility and documentation review.

If execution tools are unavailable, commit honestly, identify exact unverified risks and make validation the next priority.

## 6. PR and delivery discipline

The project owner prefers **substantial consolidated implementation batches**, not an immediate PR after every change. Work on an appropriate feature branch; do not open PRs until the milestone is coherent and validation is performed or the owner explicitly requests a PR. Avoid excessive commits with no integration outcome. Do not claim merged work unless verified.

## 7. Definition of done

A feature is done only when its intended end-to-end behavior works through the public boundary, tenant/policy constraints hold, tests and build pass, failure/recovery paths are covered, documentation and operational impacts are updated, and no unreviewed coupling undermines the Toba vision.

## 8. Agent self-check before every response

- Does this advance a real Toba lakehouse capability rather than only scaffolding?
- Is the implementation engine-agnostic and consistent with Toba/Farabi/Numu boundaries?
- Can unauthorized users or agents bypass governance?
- Are tests, integration, deployment and migration evidence accurate?
- What remains missing, and what should be implemented next?

If any answer is unclear, say so explicitly and avoid readiness claims.
