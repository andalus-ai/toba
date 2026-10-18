package com.dremioplatform.workload.repository;

import com.dremioplatform.workload.domain.WlmQueueRule;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class WlmQueueRuleRepository implements PanacheRepositoryBase<WlmQueueRule, UUID> {

    /** Priority-ordered, matching the index — see WlmQueueRule's Javadoc for match semantics. */
    public Uni<List<WlmQueueRule>> findEnabledOrderedByPriority() {
        return find("enabled = true order by priority desc, createdAt asc").list();
    }

    public Uni<List<WlmQueueRule>> findByEngine(UUID engineId) {
        return find("engineId", engineId).list();
    }
}
