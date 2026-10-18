package com.dremioplatform.workload.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateWlmQueueRuleRequest(
        @NotBlank String name,
        @NotNull UUID engineId,
        int priority,
        @NotBlank String queryTagPattern,
        int maxConcurrentQueries,
        int queueTimeoutSeconds
) {
}
