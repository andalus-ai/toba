package com.dremioplatform.toba.engine;

import java.util.Objects;
import java.util.UUID;

/** Opaque downstream execution reference; treat as internal, never as a user credential. */
public record EngineQueryHandle(UUID queryId, String engineId, String engineJobId) {
    public EngineQueryHandle {
        Objects.requireNonNull(queryId, "queryId");
        engineId = required(engineId, "engineId");
        engineJobId = required(engineJobId, "engineJobId");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}
