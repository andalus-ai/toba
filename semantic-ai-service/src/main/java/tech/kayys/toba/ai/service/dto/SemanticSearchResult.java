package com.dremioplatform.ai.service.dto;

import com.dremioplatform.ai.domain.DatasetEmbedding;
import com.dremioplatform.ai.domain.EmbeddingTargetType;
import java.util.UUID;

public record SemanticSearchResult(
        UUID id, String datasetPath, String columnName, EmbeddingTargetType targetType,
        String sourceText, double similarityScore
) {
    public static SemanticSearchResult from(DatasetEmbedding e) {
        return new SemanticSearchResult(e.id, e.datasetPath, e.columnName, e.targetType,
                e.sourceText, e.similarityScore == null ? 0.0 : e.similarityScore);
    }
}
