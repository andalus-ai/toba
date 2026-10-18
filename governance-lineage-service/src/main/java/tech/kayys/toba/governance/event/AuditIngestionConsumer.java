package com.dremioplatform.governance.event;

import com.dremioplatform.governance.domain.AuditLogEntry;
import com.dremioplatform.governance.repository.AuditLogEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Subscribes to every service's domain-event topic and mirrors each event into
 * the append-only audit_log_entry table (plan Section 5.2, "Audit history: consumes
 * every job/query/DDL event"). Only identity.events is wired today, matching what
 * identity-access-service actually publishes; catalog.events and dremio.job-events
 * consumers are added the same way once those producers exist — this consumer's
 * shape (parse JSON, extract eventId for idempotency, persist) doesn't change.
 *
 * Idempotent on sourceEventId: Kafka delivery is at-least-once, so a redelivered
 * message after a consumer restart must not create a duplicate audit row.
 */
@ApplicationScoped
public class AuditIngestionConsumer {

    private static final Logger LOG = Logger.getLogger(AuditIngestionConsumer.class);

    @Inject AuditLogEntryRepository auditLogEntryRepository;
    @Inject ObjectMapper objectMapper;

    @Incoming("identity-events-in")
    @WithTransaction
    public Uni<Void> onIdentityEvent(String rawJson) {
        return ingest("identity-access-service", rawJson);
    }

    private Uni<Void> ingest(String sourceService, String rawJson) {
        JsonNode node;
        try {
            node = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            LOG.errorf(e, "Skipping unparseable %s event: %s", sourceService, rawJson);
            return Uni.createFrom().voidItem();
        }

        UUID eventId = UUID.fromString(node.path("eventId").asText());

        return auditLogEntryRepository.existsBySourceEventId(eventId)
                .chain(alreadyIngested -> {
                    if (alreadyIngested) {
                        return Uni.createFrom().voidItem();
                    }
                    AuditLogEntry entry = new AuditLogEntry();
                    entry.id = UUID.randomUUID();
                    entry.sourceEventId = eventId;
                    entry.sourceService = sourceService;
                    entry.eventType = node.path("eventType").isMissingNode()
                            ? guessEventType(node) : node.path("eventType").asText();
                    entry.actorPrincipalType = textOrNull(node, "principalType", "actorPrincipalType");
                    entry.actorPrincipalId = textOrNull(node, "principalId", "actorUserId", "userId");
                    entry.resourcePath = textOrNull(node, "scopePath", "resourcePath");
                    entry.detailsJson = rawJson;
                    entry.occurredAt = node.hasNonNull("occurredAt")
                            ? Instant.parse(node.get("occurredAt").asText()) : Instant.now();
                    return auditLogEntryRepository.persist(entry).replaceWithVoid();
                });
    }

    private static String guessEventType(JsonNode node) {
        if (node.has("changeType")) return "RoleAssignment" + node.get("changeType").asText();
        if (node.has("transition")) return node.get("transition").asText();
        return "Unknown";
    }

    private static String textOrNull(JsonNode node, String... candidateFields) {
        for (String field : candidateFields) {
            if (node.hasNonNull(field)) {
                return node.get(field).asText();
            }
        }
        return null;
    }
}
