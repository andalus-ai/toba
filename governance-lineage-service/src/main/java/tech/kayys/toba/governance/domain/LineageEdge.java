package com.dremioplatform.governance.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One edge in the column/table lineage graph: sourcePath produced (part of)
 * targetPath, observed from a specific job's Calcite plan. Column-level lineage
 * sets both *Column fields; table-level lineage (CTAS, view creation) leaves them
 * null. Stored flat (source/target columns, not a normalized node table) so the
 * recursive-CTE upstream/downstream traversal described in the plan (Section 5.2)
 * is a single self-join query — see LineageRepository.
 */
@Entity
@Table(name = "lineage_edge")
public class LineageEdge extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String sourcePath;
    public String sourceColumn;

    @Column(nullable = false)
    public String targetPath;
    public String targetColumn;

    /** e.g. "JOIN", "AGGREGATE", "CTAS", "VIEW_DEFINITION" — human-readable transform kind for the lineage UI. */
    public String transformationType;

    /** The Dremio job id this edge was derived from, for traceability/debugging. */
    public String sourceJobId;

    @Column(nullable = false)
    public Instant observedAt = Instant.now();
}
