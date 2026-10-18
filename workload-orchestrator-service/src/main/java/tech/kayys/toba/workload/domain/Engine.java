package com.dremioplatform.workload.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * A logical pool of executor pods (plan Section 5.4, "Elastic 'engine' concept").
 * `minReplicas=0` is what enables true auto-stop (the engine can scale to zero when
 * idle, not just scale down to a floor). `k8sNamespace`/`k8sDeploymentName` identify
 * the actual K8s Deployment/StatefulSet this engine's replica count controls — see
 * {@link com.dremioplatform.workload.k8s.K8sScalingClient}.
 *
 * `version` (optimistic locking) matters here specifically because both the REST
 * API (manual start/stop/scale) and the idle-auto-stop scheduled job can race to
 * mutate the same engine row concurrently.
 */
@Entity
@Table(name = "engine")
public class Engine extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String name;

    public String description;

    @Column(nullable = false)
    public int minReplicas = 0;

    @Column(nullable = false)
    public int maxReplicas = 10;

    @Column(nullable = false)
    public int currentReplicas = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EngineStatus status = EngineStatus.STOPPED;

    @Column(nullable = false)
    public boolean autoStopEnabled = true;

    @Column(nullable = false)
    public int autoStopIdleMinutes = 15;

    /** Updated every time a query is routed to this engine (see WlmQueueRuleService /
     *  the not-yet-built job-submission hook) — the idle-auto-stop scheduler's clock. */
    public Instant lastActivityAt;

    @Column(nullable = false)
    public String k8sNamespace;

    @Column(nullable = false)
    public String k8sDeploymentName;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    @Version
    public long version;
}
