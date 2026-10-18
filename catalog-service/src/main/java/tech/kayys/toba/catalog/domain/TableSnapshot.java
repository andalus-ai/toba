package com.dremioplatform.catalog.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One entry in a table's snapshot history — what "time travel" (plan Section 5.3)
 * and the not-yet-built ExpireSnapshots compaction job both operate over. Kept as
 * its own append-only table (never updated, only inserted) rather than embedding
 * history in CatalogTable, since a table can accumulate thousands of snapshots and
 * every snapshot-listing/rollback query needs to scan/filter/sort them independently
 * of the parent table row.
 */
@Entity
@Table(name = "table_snapshot")
public class TableSnapshot extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public UUID catalogTableId;

    @Column(nullable = false)
    public long snapshotId;

    public Long parentSnapshotId;

    @Column(nullable = false)
    public String manifestListLocation;

    @Column(nullable = false, columnDefinition = "jsonb")
    public String summaryJson = "{}";

    @Column(nullable = false)
    public Instant committedAt = Instant.now();
}
