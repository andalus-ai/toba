package com.dremioplatform.workload.repository;

import com.dremioplatform.workload.domain.QueueDepthSample;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class QueueDepthSampleRepository implements PanacheRepositoryBase<QueueDepthSample, UUID> {

    public Uni<List<QueueDepthSample>> findRecentForEngine(UUID engineId, Instant since) {
        return find("engineId = ?1 and sampledAt >= ?2 order by sampledAt desc", engineId, since).list();
    }

    /** Every engine with at least one sample in the window — the scale-up
     *  scheduler's "who do I even need to look at" query, avoiding a full
     *  engine-table scan when most engines may have no recent activity. */
    public Uni<List<UUID>> findDistinctEngineIdsWithRecentSamples(Instant since) {
        return getSession().chain(session -> session
                .createQuery("select distinct s.engineId from QueueDepthSample s where s.sampledAt >= ?1", UUID.class)
                .setParameter(1, since)
                .getResultList());
    }

    public Uni<Long> deleteOlderThan(Instant cutoff) {
        return delete("sampledAt < ?1", cutoff);
    }
}
