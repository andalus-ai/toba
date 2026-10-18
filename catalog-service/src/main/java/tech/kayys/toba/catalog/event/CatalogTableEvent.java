package com.dremioplatform.catalog.event;

import java.time.Instant;
import java.util.UUID;

public record CatalogTableEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        String namespacePath,
        String tableName,
        UUID actorPrincipalId
) {
    public static CatalogTableEvent of(String eventType, String namespacePath, String tableName, UUID actorPrincipalId) {
        return new CatalogTableEvent(UUID.randomUUID(), Instant.now(), eventType, namespacePath, tableName, actorPrincipalId);
    }
}
