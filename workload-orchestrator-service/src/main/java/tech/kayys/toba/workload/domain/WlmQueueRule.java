package com.dremioplatform.workload.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Query-tagging + priority-queue rule (plan Section 5.4, "Custom WLM"). The
 * not-yet-built job-submission hook (adapter-side, intercepting Dremio's job
 * submission path per plan Section 7's integration-points table) tags each incoming
 * query and calls {@code WlmQueueRuleService.resolveEngineFor(tags)} to pick a
 * target engine. Rules are evaluated in descending `priority` order; the first rule
 * whose `queryTagPattern` matches wins — see WlmQueueRuleService for the match
 * semantics (simple glob, not a full regex engine, to keep rule authoring approachable
 * for admins in the future UI).
 */
@Entity
@Table(name = "wlm_queue_rule")
public class WlmQueueRule extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public UUID engineId;

    /** Higher priority evaluated first. Ties broken by createdAt ascending (first-created wins). */
    @Column(nullable = false)
    public int priority;

    /** Simple glob pattern matched against a query's tag string, e.g. "bi-tool:*" or "etl:nightly-*". */
    @Column(nullable = false)
    public String queryTagPattern;

    @Column(nullable = false)
    public int maxConcurrentQueries = 50;

    @Column(nullable = false)
    public int queueTimeoutSeconds = 300;

    @Column(nullable = false)
    public boolean enabled = true;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();
}
