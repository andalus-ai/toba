package com.dremioplatform.ai.service.dto;

import com.dremioplatform.ai.domain.WikiSuggestion;
import com.dremioplatform.ai.domain.WikiSuggestionStatus;
import java.time.Instant;
import java.util.UUID;

public record WikiSuggestionResponse(
        UUID id, String datasetPath, String columnName, String suggestedDescription,
        WikiSuggestionStatus status, String generatedByModel, Instant createdAt,
        UUID reviewedByUserId, Instant reviewedAt, String reviewNote
) {
    public static WikiSuggestionResponse from(WikiSuggestion w) {
        return new WikiSuggestionResponse(w.id, w.datasetPath, w.columnName, w.suggestedDescription,
                w.status, w.generatedByModel, w.createdAt, w.reviewedByUserId, w.reviewedAt, w.reviewNote);
    }
}
