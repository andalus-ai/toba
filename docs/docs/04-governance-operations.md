# 04 — Governance, security and operations

## Trust boundaries

- Runtime authenticates users and service accounts; the engine receives a trusted actor context.
- Workspace, subject, dataset, metric, column and row-level permissions are evaluated before execution and before publication.
- No agent, browser client, A2UI payload or chart spec may select privileged data sources, bypass filters or supply a trusted subject identity.
- Dremio/Toba credentials live in secret management, not in dashboard artifacts or logs.
- Parameterize queries; validate identifiers against the semantic catalog and enforce row limits, execution timeouts and quotas.

## Policy lifecycle

Authorization decisions should include policy version, actor, workspace, semantic assets, requested action, and allow/deny outcome. Recheck policy for artifact publishing and when executing scheduled refresh. A saved artifact does not grant permanent access to revoked data.

## Audit and lineage

Capture query request ID, execution ID, remote provider ID, source, semantic model version, actor/workspace, policy decision, execution transitions, generated chart spec, promotion event and publication version. Audit records should be append-only and correlated with distributed traces. Do not log credentials, raw sensitive data or unrestricted prompts containing secrets.

## Worker safety

- Claim bounded batches; lease owner + monotonically increasing fencing epoch.
- Only the current lease holder may transition state; persist audit in the same transaction.
- Renew/release leases with fencing checks; back off repeated failures.
- Reconcile unknown remote state without duplicate submission.
- Recover expired and orphaned jobs with explicit policy, not hidden retries.
- Ensure cancellation semantics distinguish local request, provider acknowledgement and terminal state.

## Reliability and observability

Recommended telemetry: request rate, authorization denials, query queue time, remote latency, active jobs, stale leases, reconciliation outcomes, worker attempts, dashboard render errors, promotion rate and scheduled refresh failures. Trace from HTTP/agent request to semantic compilation, policy decision, Dremio submission and persistence. Provide liveness, readiness and startup probes with different meanings.

## Release gates

- Compile and unit-test every Maven module on Java 21.
- PostgreSQL integration tests with migration application and concurrency/fencing scenarios.
- Dremio transport contract tests with stubbed HTTP responses, timeouts and unknown provider states.
- Quarkus startup/CDI wiring and authenticated API tests.
- Security tests for workspace isolation, denied access, row/column policy and agent prompt injection.
- Contract tests for embedded runtime parity and versioned chart/A2UI specifications.
- Backward-compatibility tests for legacy BI adapters.

## Operational runbooks required before production

Database migration/rollback, credential rotation, Dremio outage handling, ambiguous submissions, worker stuck lease, manual reconciliation, cancellation, dashboard version rollback, audit retention and tenant offboarding.
