package com.dremioplatform.governance.repository;

import com.dremioplatform.governance.domain.LineageEdge;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LineageEdgeRepository implements PanacheRepositoryBase<LineageEdge, UUID> {

    /**
     * One-hop upstream (what feeds targetPath) and downstream (what targetPath/
     * sourcePath feeds) lookups. Multi-hop traversal (full upstream/downstream
     * closure) is a recursive CTE — see the plan's note that this table can move to
     * a graph DB (JanusGraph/Neo4j) if traversal depth/scale outgrows Postgres CTEs;
     * that recursive query is added alongside the lineage-graph REST endpoint that
     * needs it (not yet built — see module README).
     */
    public Uni<List<LineageEdge>> findDirectUpstream(String targetPath) {
        return find("targetPath", targetPath).list();
    }

    public Uni<List<LineageEdge>> findDirectDownstream(String sourcePath) {
        return find("sourcePath", sourcePath).list();
    }
}
