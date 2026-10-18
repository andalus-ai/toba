package com.dremioplatform.ai.service.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewWikiSuggestionRequest(@NotNull Boolean approve, String reviewNote) {
}
