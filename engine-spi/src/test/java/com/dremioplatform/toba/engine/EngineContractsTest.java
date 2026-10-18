package com.dremioplatform.toba.engine;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EngineContractsTest {
    @Test void queryRejectsInvalidBoundsAndMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new EngineQuery(
                UUID.randomUUID(), "select 1", "ws", "alice", "source",
                Map.of(), Duration.ofSeconds(1), 0));
        assertThrows(IllegalArgumentException.class, () -> new EngineQuery(
                UUID.randomUUID(), "select 1", "ws", "alice", "source",
                Map.of(), Duration.ZERO, 10));
    }

    @Test void queryDefensivelyCopiesTags() {
        var tags = new java.util.HashMap<String, String>();
        tags.put("purpose", "bi");
        var query = new EngineQuery(UUID.randomUUID(), "select 1", "ws", "alice",
                "source", tags, Duration.ofSeconds(2), 100);
        tags.put("purpose", "changed");
        assertEquals("bi", query.tags().get("purpose"));
        assertThrows(UnsupportedOperationException.class, () -> query.tags().put("x", "y"));
    }

    @Test void capabilitiesAreExplicitAndImmutable() {
        var capabilities = new EngineCapabilities(Set.of(EngineCapability.ASYNC_EXECUTION));
        assertTrue(capabilities.supports(EngineCapability.ASYNC_EXECUTION));
        assertFalse(capabilities.supports(EngineCapability.TRANSACTIONS));
        assertThrows(UnsupportedOperationException.class,
                () -> capabilities.supported().add(EngineCapability.TRANSACTIONS));
    }

    @Test void resultRowsPreserveNullValuesWithoutAllowingMutation() {
        var row = new java.util.HashMap<String, Object>();
        row.put("optional", null);
        var page = new EngineResultPage(java.util.List.of(
                new EngineResultPage.Column("optional", "VARCHAR")),
                java.util.List.of(row), 0, false);
        assertTrue(page.rows().get(0).containsKey("optional"));
        assertNull(page.rows().get(0).get("optional"));
        assertThrows(UnsupportedOperationException.class,
                () -> page.rows().get(0).put("x", "y"));
    }
}
