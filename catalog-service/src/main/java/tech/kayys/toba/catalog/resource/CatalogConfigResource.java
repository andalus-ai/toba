package com.dremioplatform.catalog.resource;

import com.dremioplatform.catalog.service.dto.CatalogConfigResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * GET /v1/config — the first call any spec-compliant Iceberg REST client (including
 * Dremio's own IcebergRestCatalog StoragePlugin) makes to discover catalog-wide
 * config before any other request. No overrides/defaults are needed yet; returned
 * empty maps are still spec-valid.
 */
@Path("/v1/config")
@Tag(name = "Iceberg REST Catalog")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogConfigResource {

    @GET
    @Operation(summary = "Iceberg REST Catalog config discovery endpoint")
    public CatalogConfigResponse config() {
        return new CatalogConfigResponse(Map.of(), Map.of());
    }
}
