package com.dremioplatform.toba.engine;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A trusted, already-authorized query request. This SPI is not an authorization
 * boundary: callers must enforce identity, policy and query validation before submit.
 */
public record EngineQuery(UUID queryId, String statement, String workspaceId,
                          String principalId, String sourceId, Map<String, String> tags,
                          Duration timeout, int maxRows) {
    public EngineQuery {
        Objects.requireNonNull(queryId, "queryId");
        statement = required(statement, "statement");
        workspaceId = required(workspaceId, "workspaceId");
        principalId = required(principalId, "principalId");
        sourceId = required(sourceId, "sourceId");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (maxRows < 1 || maxRows > 1_000_000) {
            throw new IllegalArgumentException("maxRows must be between 1 and 1000000");
        }
        tags = tags == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(tags));
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}
