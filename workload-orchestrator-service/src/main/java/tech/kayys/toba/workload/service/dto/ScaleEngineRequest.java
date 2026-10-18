package com.dremioplatform.workload.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScaleEngineRequest(@NotNull @Min(0) Integer targetReplicas, String reason) {
}
