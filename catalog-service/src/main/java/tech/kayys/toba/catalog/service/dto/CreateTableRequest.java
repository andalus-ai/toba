package com.dremioplatform.catalog.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * `schemaJson` is now a REAL Iceberg schema — the exact JSON shape
 * `org.apache.iceberg.SchemaParser.fromJson(String)` parses (a `type: "struct"`
 * object with a `fields` array; see Apache Iceberg's own schema JSON spec). This
 * replaced an earlier version of this DTO where `schemaJson` was stored opaquely
 * and never interpreted — see {@code TableService}'s Javadoc for what's still
 * simplified (partition spec is always unpartitioned; sort order is always
 * unsorted — this DTO has no fields for either yet).
 *
 * Example value: {"type":"struct","fields":[{"id":1,"name":"id","required":true,"type":"long"},
 * {"id":2,"name":"name","required":false,"type":"string"}]}
 */
public record CreateTableRequest(
        @NotBlank String name,
        @NotBlank String schemaJson,
        Map<String, String> properties
) {
}
