package com.dremioplatform.workload.repository;

import com.dremioplatform.workload.domain.Engine;
import com.dremioplatform.workload.domain.EngineStatus;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EngineRepository implements PanacheRepositoryBase<Engine, UUID> {

    public Uni<Engine> findByName(String name) {
        return find("name", name).firstResult();
    }

    public Uni<Boolean> existsByName(String name) {
        return count("name", name).map(c -> c > 0);
    }

    /** The idle-auto-stop scheduler's query — see the matching partial index in
     *  V1__init_workload_schema.sql. */
    public Uni<List<Engine>> findRunningWithAutoStopEnabled() {
        return find("status = ?1 and autoStopEnabled = true", EngineStatus.RUNNING).list();
    }
}
