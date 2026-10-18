package com.dremioplatform.catalog.service.dto;

import java.util.List;

public record ListTablesResponse(List<TableIdentifier> identifiers) {
    public record TableIdentifier(List<String> namespace, String name) {
    }
}
