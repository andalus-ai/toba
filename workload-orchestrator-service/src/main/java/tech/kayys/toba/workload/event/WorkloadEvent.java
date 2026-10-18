package com.dremioplatform.workload.event;

import com.dremioplatform.workload.domain.EngineScalingEventType;
import java.time.Instant;
import java.util.UUID;

public record WorkloadEvent(
        UUID eventId,
        Instant occurredAt,
        UUID engineId,
        EngineScalingEventType eventType,
        int fromReplicas,
        int toReplicas,
        String reason,
        UUID actorPrincipalId
) {
    public static WorkloadEvent of(UUID engineId, EngineScalingEventType eventType, int fromReplicas,
                                     int toReplicas, String reason, UUID actorPrincipalId) {
        return new WorkloadEvent(UUID.randomUUID(), Instant.now(), engineId, eventType,
                fromReplicas, toReplicas, reason, actorPrincipalId);
    }
}
