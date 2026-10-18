package com.dremioplatform.ai.service.dto;

import com.dremioplatform.ai.domain.EmbeddingTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertEmbeddingRequest(
        @NotBlank String datasetPath,
        String columnName,
        @NotNull EmbeddingTargetType targetType,
        @NotBlank String sourceText
) {
}
