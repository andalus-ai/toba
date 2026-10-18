package com.dremioplatform.governance.service;

import com.dremioplatform.governance.domain.LineageEdge;
import com.dremioplatform.governance.repository.LineageEdgeRepository;
import com.dremioplatform.governance.service.dto.CreateLineageEdgeRequest;
import com.dremioplatform.governance.service.dto.LineageEdgeResponse;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LineageService {

    @Inject LineageEdgeRepository lineageEdgeRepository;

    /** Called by the (forthcoming) job-plan-event consumer once the dremio-adapter
     *  publishes Calcite plan JSON to catalog/job event topics — each plan produces
     *  one or more edges. Exposed as a direct REST endpoint too so edges can be
     *  backfilled/corrected manually during rollout. */
    @WithTransaction
    public Uni<LineageEdgeResponse> recordEdge(CreateLineageEdgeRequest request) {
        LineageEdge edge = new LineageEdge();
        edge.id = UUID.randomUUID();
        edge.sourcePath = request.sourcePath();
        edge.sourceColumn = request.sourceColumn();
        edge.targetPath = request.targetPath();
        edge.targetColumn = request.targetColumn();
        edge.transformationType = request.transformationType();
        edge.sourceJobId = request.sourceJobId();
        return lineageEdgeRepository.persist(edge).map(LineageEdgeResponse::from);
    }

    public Uni<List<LineageEdgeResponse>> upstreamOf(String targetPath) {
        return lineageEdgeRepository.findDirectUpstream(targetPath)
                .map(list -> list.stream().map(LineageEdgeResponse::from).toList());
    }

    public Uni<List<LineageEdgeResponse>> downstreamOf(String sourcePath) {
        return lineageEdgeRepository.findDirectDownstream(sourcePath)
                .map(list -> list.stream().map(LineageEdgeResponse::from).toList());
    }
}
