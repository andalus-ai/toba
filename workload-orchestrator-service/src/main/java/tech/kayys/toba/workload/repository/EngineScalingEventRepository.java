package com.dremioplatform.workload.repository;

import com.dremioplatform.workload.domain.EngineScalingEvent;
import com.dremioplatform.workload.domain.EngineScalingEventType;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EngineScalingEventRepository implements PanacheRepositoryBase<EngineScalingEvent, UUID> {

    public Uni<List<EngineScalingEvent>> findByEngine(UUID engineId, int limit) {
        return find("engineId = ?1 order by occurredAt desc", engineId).page(0, limit).list();
    }

    /** {@link com.dremioplatform.workload.scheduler.ScaleUpScheduler}'s cooldown
     *  check: the most recent upward event (START or SCALE_UP) for this engine, so
     *  it doesn't scale up again while a previous scale-up is still settling. */
    public Uni<EngineScalingEvent> findMostRecentUpwardEvent(UUID engineId) {
        return find("engineId = ?1 and eventType in (?2, ?3) order by occurredAt desc",
                engineId, EngineScalingEventType.STARTED, EngineScalingEventType.SCALED_UP)
                .firstResult();
    }
}
