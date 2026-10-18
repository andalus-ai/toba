# Workload Orchestration Service

Phase 4 of the roadmap in `dremio-reactive-extension-plan.md`. Owns elastic engine
pools, workload-management (WLM) query routing, and the K8s API calls that scale
executor pods.

## Status

| # | Layer | Status |
|---|---|---|
| 1 | Domain model + migration | ✅ done |
| 2 | Repository layer | ✅ done |
| 3 | Service layer (`EngineService`, `WlmQueueRuleService`) | ✅ done |
| 4 | REST layer | ✅ done |
| 5 | K8s scaling client + idle-auto-stop scheduled job + queue-depth scale-up scheduled job | ✅ done |
| 6 | Job-submission routing hook (adapter-side) | not started |
| 7 | Tests | 🟡 started — REST-integration coverage of start/stop/scale, WLM rule resolution, and end-to-end queue-depth-triggered scale-up (`ScaleUpFlowTest`, running the real scheduler at a fast test interval via `ScaleUpSchedulerTestProfile`) |

## What's built

- **`Engine`** — a logical executor pool with `minReplicas`/`maxReplicas`,
  `autoStopEnabled`/`autoStopIdleMinutes`, and a `k8sNamespace`/`k8sDeploymentName`
  pointing at the real K8s object it controls. Optimistic locking (`@Version`)
  matters here specifically because the REST API and the idle-auto-stop scheduler
  can race to mutate the same row.
- **`K8sScalingClient`** — scales a Deployment (falling back to StatefulSet) via
  `quarkus-kubernetes-client`. **Defaults to a logged no-op** (`workload.k8s.enabled=false`)
  outside an explicit opt-in — a safety default, not just a dev convenience, so a
  locally-run instance pointed at the wrong kubeconfig can't accidentally scale a
  real cluster. All three integration tests run against this no-op path.
- **`EngineService.doScale`** — the shared core of start/stop/scale: calls K8s
  first, and only updates the `Engine` row + records the audit event if that call
  succeeds, avoiding a "database says running, cluster says stopped" split-brain.
- **`WlmQueueRuleService.resolveEngineFor`** — the query-tag routing decision, using
  a small hand-rolled glob matcher (`*`/`?`) rather than full regex, so rule authoring
  stays approachable for a future admin UI. Rules evaluated in descending priority.
- **`IdleAutoStopScheduler`** — a `@Scheduled` job (default: every minute) that
  finds every `RUNNING`, auto-stop-enabled engine past its idle threshold and scales
  it to zero through the same K8s-call-then-record path `EngineService` uses for a
  manual stop, tagged `AUTO_STOPPED_IDLE` in its history for traceability.
- **`EngineScalingEvent`** — append-only history of every scaling action, manual or
  automatic, also published to `workload.events` Kafka topic for Governance's audit
  ingestion (once that service adds a consumer for it — same pattern as the other
  services' event publishers).

## Known gaps / next continuation prompts

- **Queue-depth-triggered scale-up is implemented** (`ScaleUpScheduler`) — the
  other half of "auto-start/stop ... based on queue depth/queued-query metrics"
  (plan Section 5.4) that idle-timeout-only V1 didn't cover. Averages
  `QueueDepthSample.queuedQueryCount` over a rolling window
  (`workload.scaleup.evaluation-window`, default 2 min); scales up by
  `workload.scaleup.increment` (capped at `maxReplicas`) when the average exceeds
  `workload.scaleup.queue-depth-threshold`, unless still within
  `workload.scaleup.cooldown` of a previous scale-up (prevents flapping while new
  pods are becoming ready). Reuses `EngineService.scale` directly — same
  K8s-call-then-record-then-rollback-on-failure path a human-triggered scale uses,
  just with a null `actorPrincipalId` to distinguish scheduler-triggered events in
  the audit trail. Samples are reported via `POST .../queue-depth-samples`, the
  exact shape the not-yet-built job-submission hook would call.
- **No job-submission routing hook still means no *real* queue-depth data.**
  `ScaleUpScheduler`'s logic is real and tested end-to-end, but nothing in this
  repo yet reports genuine queue depth from an actual Dremio coordinator — that's
  the same missing piece `WlmQueueRuleService.resolveEngineFor` has been waiting
  on since V1.
- **No job-submission routing hook.** `WlmQueueRuleService.resolveEngineFor` and
  `EngineService.recordActivity` both exist and are REST-callable, but nothing in
  the `dremio-adapter` module calls them yet — Dremio's job-submission interception
  point (plan Section 7's integration-points table) needs the same kind of
  reconciliation-against-real-SPI treatment as `dremio-adapter`'s access-control
  piece.
- **No replica-count drift correction.** `K8sScalingClient.currentReadyReplicas`
  exists for this but nothing calls it on a schedule yet — if K8s reports a
  different ready-replica count than `Engine.currentReplicas` (e.g. a pod crash-
  loop), this service won't currently notice.
- **Kubernetes-role RBAC for the service account this service runs as** isn't
  specified here — a real deployment needs a `Role`/`RoleBinding` scoping this
  service's K8s permissions to exactly `get`/`scale` on Deployments/StatefulSets in
  the execution-plane namespace, nothing broader.

## Running locally

```bash
mvn -pl workload-orchestrator-service -am quarkus:dev
```

No real K8s cluster needed — `workload.k8s.enabled` defaults to `false`, so every
scaling call logs what it *would* do instead of calling a real API. Set
`WORKLOAD_K8S_ENABLED=true` (and point `quarkus.kubernetes-client.*` at a real
kubeconfig) to test against an actual cluster.

## Next continuation prompt

**"Add replica-count drift correction to workload-orchestrator-service"** —
`K8sScalingClient.currentReadyReplicas` exists but nothing calls it on a schedule;
if K8s reports a different ready-replica count than `Engine.currentReplicas` (a pod
crash-loop, a manual `kubectl scale`), this service currently won't notice.
