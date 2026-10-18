package com.dremioplatform.ai.service.dto;

import com.dremioplatform.ai.domain.EmbeddingTargetType;
import jakarta.validation.constraints.NotBlank;

public record SemanticSearchRequest(@NotBlank String query, EmbeddingTargetType targetType, Integer limit) {
}
