package com.dremioplatform.catalog.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * An Iceberg REST Catalog "namespace" — a multi-level path, e.g. ["sales", "q4"].
 * Stored as a single dot-joined string (`levelsPath`) rather than a normalized
 * levels table, since every operation addresses a namespace by its full path, never
 * by individual level. The Iceberg REST spec itself joins levels with a unit
 * separator (0x1F) in the URL path — see {@link com.dremioplatform.catalog.util.NamespaceCodec}
 * for the encode/decode between that wire format and this storage format.
 */
@Entity
@Table(name = "catalog_namespace")
public class CatalogNamespace extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String levelsPath;

    @Column(nullable = false, columnDefinition = "jsonb")
    public String propertiesJson = "{}";

    @Column(nullable = false)
    public Instant createdAt = Instant.now();
}
