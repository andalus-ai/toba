package com.dremioplatform.catalog.service.dto;

import java.util.Map;

/** Mirrors the Iceberg REST spec's GET /v1/config response — clients call this
 *  first to discover any catalog-wide default/override properties. */
public record CatalogConfigResponse(Map<String, String> defaults, Map<String, String> overrides) {
}
