package com.dremioplatform.workload.service.dto;

import com.dremioplatform.workload.domain.Engine;
import com.dremioplatform.workload.domain.EngineStatus;
import java.time.Instant;
import java.util.UUID;

public record EngineResponse(
        UUID id, String name, String description, int minReplicas, int maxReplicas, int currentReplicas,
        EngineStatus status, boolean autoStopEnabled, int autoStopIdleMinutes, Instant lastActivityAt,
        String k8sNamespace, String k8sDeploymentName, Instant createdAt
) {
    public static EngineResponse from(Engine e) {
        return new EngineResponse(e.id, e.name, e.description, e.minReplicas, e.maxReplicas, e.currentReplicas,
                e.status, e.autoStopEnabled, e.autoStopIdleMinutes, e.lastActivityAt,
                e.k8sNamespace, e.k8sDeploymentName, e.createdAt);
    }
}
