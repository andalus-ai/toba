package com.dremioplatform.governance.repository;

import com.dremioplatform.governance.domain.AuditLogEntry;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AuditLogEntryRepository implements PanacheRepositoryBase<AuditLogEntry, UUID> {

    public Uni<Boolean> existsBySourceEventId(UUID sourceEventId) {
        return count("sourceEventId", sourceEventId).map(c -> c > 0);
    }

    public Uni<List<AuditLogEntry>> findByResourcePath(String resourcePath, Instant since, Instant until, int limit) {
        return find("resourcePath = ?1 and occurredAt >= ?2 and occurredAt <= ?3 order by occurredAt desc",
                resourcePath, since, until).page(0, limit).list();
    }

    public Uni<List<AuditLogEntry>> findByActor(String actorPrincipalType, String actorPrincipalId, Instant since, Instant until, int limit) {
        return find("actorPrincipalType = ?1 and actorPrincipalId = ?2 and occurredAt >= ?3 and occurredAt <= ?4 order by occurredAt desc",
                actorPrincipalType, actorPrincipalId, since, until).page(0, limit).list();
    }
}
