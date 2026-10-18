package com.dremioplatform.workload.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateEngineRequest(
        @NotBlank String name,
        String description,
        @Min(0) int minReplicas,
        @Min(1) int maxReplicas,
        boolean autoStopEnabled,
        @Min(1) int autoStopIdleMinutes,
        @NotBlank String k8sNamespace,
        @NotBlank String k8sDeploymentName
) {
}
