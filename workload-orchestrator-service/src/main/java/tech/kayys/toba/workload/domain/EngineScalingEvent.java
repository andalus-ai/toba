package com.dremioplatform.workload.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only history of every scaling action taken against an engine — manual
 * (REST-driven) or automatic (idle-timeout scheduler). This is the audit trail for
 * "why did this engine's replica count change", and doubles as the source data for
 * a future cost/utilization dashboard. Also published to Kafka (`workload.events`)
 * for Governance's audit ingestion, same pattern as the other services.
 */
@Entity
@Table(name = "engine_scaling_event")
public class EngineScalingEvent extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public UUID engineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EngineScalingEventType eventType;

    @Column(nullable = false)
    public int fromReplicas;

    @Column(nullable = false)
    public int toReplicas;

    public String reason;

    public UUID actorPrincipalId;

    @Column(nullable = false)
    public Instant occurredAt = Instant.now();
}
