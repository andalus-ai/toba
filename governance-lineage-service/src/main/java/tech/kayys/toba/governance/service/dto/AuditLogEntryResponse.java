package com.dremioplatform.governance.service.dto;

import com.dremioplatform.governance.domain.AuditLogEntry;
import java.time.Instant;
import java.util.UUID;

public record AuditLogEntryResponse(
        UUID id, String sourceService, String eventType, String actorPrincipalType,
        String actorPrincipalId, String resourcePath, String detailsJson, Instant occurredAt
) {
    public static AuditLogEntryResponse from(AuditLogEntry e) {
        return new AuditLogEntryResponse(e.id, e.sourceService, e.eventType, e.actorPrincipalType,
                e.actorPrincipalId, e.resourcePath, e.detailsJson, e.occurredAt);
    }
}
