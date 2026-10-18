package com.dremioplatform.governance.service.dto;

import com.dremioplatform.governance.domain.LineageEdge;
import java.time.Instant;
import java.util.UUID;

public record LineageEdgeResponse(
        UUID id, String sourcePath, String sourceColumn, String targetPath, String targetColumn,
        String transformationType, String sourceJobId, Instant observedAt
) {
    public static LineageEdgeResponse from(LineageEdge e) {
        return new LineageEdgeResponse(e.id, e.sourcePath, e.sourceColumn, e.targetPath, e.targetColumn,
                e.transformationType, e.sourceJobId, e.observedAt);
    }
}
