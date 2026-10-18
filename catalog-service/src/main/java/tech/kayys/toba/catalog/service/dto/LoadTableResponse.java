package com.dremioplatform.catalog.service.dto;

import java.util.Map;

/**
 * `metadataJson` is now the REAL Iceberg `TableMetadata` JSON (produced by
 * `org.apache.iceberg.TableMetadataParser.toJson`), read back from
 * {@code ObjectStorageClient} on every load — not a placeholder. Still simplified
 * relative to the full Iceberg REST spec's `LoadTableResponse` (which nests this
 * under a `metadata` field alongside a separate `metadata-location` and a
 * `config` map, and supports `X-Iceberg-Access-Delegation` credential vending) —
 * see `TableService`'s Javadoc for the precise boundary.
 */
public record LoadTableResponse(
        String metadataLocation,
        Long currentSnapshotId,
        Map<String, String> properties,
        String metadataJson
) {
}
