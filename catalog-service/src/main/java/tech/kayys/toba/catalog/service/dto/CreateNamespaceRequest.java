package com.dremioplatform.catalog.service.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/** Mirrors the Iceberg REST spec's CreateNamespaceRequest body shape. */
public record CreateNamespaceRequest(
        @NotEmpty List<String> namespace,
        Map<String, String> properties
) {
}
