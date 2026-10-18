package com.dremioplatform.catalog.repository;

import com.dremioplatform.catalog.domain.TableSnapshot;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TableSnapshotRepository implements PanacheRepositoryBase<TableSnapshot, UUID> {

    public Uni<List<TableSnapshot>> listForTable(UUID catalogTableId) {
        return find("catalogTableId = ?1 order by committedAt desc", catalogTableId).list();
    }

    public Uni<TableSnapshot> findByTableAndSnapshotId(UUID catalogTableId, long snapshotId) {
        return find("catalogTableId = ?1 and snapshotId = ?2", catalogTableId, snapshotId).firstResult();
    }
}
