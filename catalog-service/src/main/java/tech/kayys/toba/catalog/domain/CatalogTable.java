package com.dremioplatform.catalog.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * An Iceberg table registered in this catalog. `metadataLocation` points at the
 * table's current `TableMetadata` JSON file in object storage (S3/ADLS/GCS) — this
 * service is the catalog of record for *where that file is*, not a re-implementation
 * of Iceberg's metadata format itself (table creation in this increment writes a
 * minimal placeholder metadata document; see module README).
 */
@Entity
@Table(name = "catalog_table", uniqueConstraints = @UniqueConstraint(columnNames = {"namespace_path", "table_name"}))
public class CatalogTable extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "namespace_path", nullable = false)
    public String namespacePath;

    @Column(name = "table_name", nullable = false)
    public String tableName;

    @Column(nullable = false)
    public String metadataLocation;

    @Column(nullable = false, columnDefinition = "jsonb")
    public String propertiesJson = "{}";

    /** Null until the first snapshot commits. */
    public Long currentSnapshotId;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant lastUpdatedAt = Instant.now();
}
