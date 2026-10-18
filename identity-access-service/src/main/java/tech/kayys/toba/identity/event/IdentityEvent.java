package com.dremioplatform.identity.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker for every event this service publishes to the platform event bus (Kafka
 * topic "identity.events", see application.properties). Consumed by: the Governance
 * Service (audit trail — every event here is also an audit-log entry), and the
 * dremio-adapter / API Gateway to invalidate short-TTL authz caches on role changes.
 */
public interface IdentityEvent {
    UUID eventId();
    Instant occurredAt();
}
