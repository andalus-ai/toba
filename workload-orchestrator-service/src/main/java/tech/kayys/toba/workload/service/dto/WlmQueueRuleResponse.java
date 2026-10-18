package com.dremioplatform.workload.service.dto;

import com.dremioplatform.workload.domain.WlmQueueRule;
import java.time.Instant;
import java.util.UUID;

public record WlmQueueRuleResponse(
        UUID id, String name, UUID engineId, int priority, String queryTagPattern,
        int maxConcurrentQueries, int queueTimeoutSeconds, boolean enabled, Instant createdAt
) {
    public static WlmQueueRuleResponse from(WlmQueueRule r) {
        return new WlmQueueRuleResponse(r.id, r.name, r.engineId, r.priority, r.queryTagPattern,
                r.maxConcurrentQueries, r.queueTimeoutSeconds, r.enabled, r.createdAt);
    }
}
