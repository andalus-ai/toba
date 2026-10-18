package com.dremioplatform.catalog.service.dto;

import java.util.List;
import java.util.Map;

/** Mirrors the Iceberg REST spec's GetNamespaceResponse shape. */
public record NamespaceResponse(List<String> namespace, Map<String, String> properties) {
}
