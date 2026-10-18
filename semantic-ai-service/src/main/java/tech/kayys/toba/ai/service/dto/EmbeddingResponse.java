package com.dremioplatform.ai.service.dto;

import com.dremioplatform.ai.domain.DatasetEmbedding;
import com.dremioplatform.ai.domain.EmbeddingTargetType;
import java.time.Instant;
import java.util.UUID;

public record EmbeddingResponse(
        UUID id, String datasetPath, String columnName, EmbeddingTargetType targetType,
        String sourceText, String embeddingModel, Instant createdAt, Instant updatedAt
) {
    public static EmbeddingResponse from(DatasetEmbedding e) {
        return new EmbeddingResponse(e.id, e.datasetPath, e.columnName, e.targetType,
                e.sourceText, e.embeddingModel, e.createdAt, e.updatedAt);
    }
}
