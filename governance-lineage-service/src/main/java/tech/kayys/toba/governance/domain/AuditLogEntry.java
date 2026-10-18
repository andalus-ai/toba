package com.dremioplatform.governance.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit trail entry. Rows are never updated or deleted (compliance
 * evidence for SOC2/HIPAA/GDPR — see plan Section 5.2) — the service layer only
 * ever inserts. Ingested from the platform event bus (every microservice's domain
 * events double as audit-log source material) plus Dremio job/DDL events consumed
 * directly off the "dremio.job-events" topic (adapter-published, not yet wired —
 * see module README).
 */
@Entity
@Table(name = "audit_log_entry")
public class AuditLogEntry extends PanacheEntityBase {

    @Id
    public UUID id;

    /** Originating event's own id, used for idempotent ingestion (Kafka is at-least-once). */
    @Column(nullable = false, unique = true)
    public UUID sourceEventId;

    @Column(nullable = false)
    public String sourceService;

    @Column(nullable = false)
    public String eventType;

    public String actorPrincipalType;
    public String actorPrincipalId;

    /** Dataset/namespace path or other resource identifier this event concerns, if any. */
    public String resourcePath;

    @Column(nullable = false, columnDefinition = "jsonb")
    public String detailsJson;

    @Column(nullable = false)
    public Instant occurredAt;

    @Column(nullable = false)
    public Instant ingestedAt = Instant.now();
}
