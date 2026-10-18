package com.dremioplatform.catalog.service.dto;

import java.util.List;

/** Mirrors the Iceberg REST spec's ListNamespacesResponse shape (pagination
 *  ("next-page-token") is not yet implemented — see module README). */
public record ListNamespacesResponse(List<List<String>> namespaces) {
}
