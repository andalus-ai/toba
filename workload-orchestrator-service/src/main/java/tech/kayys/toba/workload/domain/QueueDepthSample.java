package com.dremioplatform.workload.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One point-in-time reading of an engine's query queue — the input the plan's
 * "auto-start/stop ... based on queue depth/queued-query metrics" (Section 5.4)
 * needs and V1 didn't have. Reported by whoever submits jobs to this engine — in
 * a real deployment, the not-yet-built job-submission routing hook (the same one
 * `WlmQueueRuleService.resolveEngineFor` exists for); in this increment, via
 * `POST /api/v1/engines/{id}/queue-depth-samples` directly, which is that future
 * hook's exact target shape.
 *
 * Deliberately short-lived data: {@link com.dremioplatform.workload.scheduler.ScaleUpScheduler}
 * prunes samples older than its evaluation window on every run, so this table
 * holds minutes of rolling history, not a permanent log — `EngineScalingEvent` is
 * the permanent audit trail; this is just the scale-up evaluator's working set.
 */
@Entity
@Table(name = "queue_depth_sample")
public class QueueDepthSample extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public UUID engineId;

    @Column(nullable = false)
    public int queuedQueryCount;

    @Column(nullable = false)
    public int runningQueryCount;

    @Column(nullable = false)
    public Instant sampledAt = Instant.now();
}
