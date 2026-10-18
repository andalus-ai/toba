package com.dremioplatform.workload.service.dto;

import com.dremioplatform.workload.domain.EngineScalingEvent;
import com.dremioplatform.workload.domain.EngineScalingEventType;
import java.time.Instant;
import java.util.UUID;

public record EngineScalingEventResponse(
        UUID id, UUID engineId, EngineScalingEventType eventType, int fromReplicas, int toReplicas,
        String reason, Instant occurredAt
) {
    public static EngineScalingEventResponse from(EngineScalingEvent e) {
        return new EngineScalingEventResponse(e.id, e.engineId, e.eventType, e.fromReplicas, e.toReplicas,
                e.reason, e.occurredAt);
    }
}
