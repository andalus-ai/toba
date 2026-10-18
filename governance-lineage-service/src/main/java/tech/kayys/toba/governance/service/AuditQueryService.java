package com.dremioplatform.governance.service;

import com.dremioplatform.governance.repository.AuditLogEntryRepository;
import com.dremioplatform.governance.service.dto.AuditLogEntryResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class AuditQueryService {

    private static final int DEFAULT_LIMIT = 200;

    @Inject AuditLogEntryRepository auditLogEntryRepository;

    public Uni<List<AuditLogEntryResponse>> byResource(String resourcePath, Instant since, Instant until) {
        return auditLogEntryRepository
                .findByResourcePath(resourcePath, orDefaultSince(since), orDefaultUntil(until), DEFAULT_LIMIT)
                .map(list -> list.stream().map(AuditLogEntryResponse::from).toList());
    }

    public Uni<List<AuditLogEntryResponse>> byActor(String actorPrincipalType, String actorPrincipalId, Instant since, Instant until) {
        return auditLogEntryRepository
                .findByActor(actorPrincipalType, actorPrincipalId, orDefaultSince(since), orDefaultUntil(until), DEFAULT_LIMIT)
                .map(list -> list.stream().map(AuditLogEntryResponse::from).toList());
    }

    private static Instant orDefaultSince(Instant since) {
        return since != null ? since : Instant.EPOCH;
    }

    private static Instant orDefaultUntil(Instant until) {
        return until != null ? until : Instant.now();
    }
}
