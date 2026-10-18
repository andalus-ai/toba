package com.dremioplatform.ai.service.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateWikiSuggestionRequest(
        @NotBlank String datasetPath,
        String columnName,
        /** Schema/sample-data/query-history context the caller supplies for the
         *  prompt — this service doesn't itself pull data from Dremio or the
         *  Catalog Service yet, see module README "Known gaps". */
        @NotBlank String context
) {
}
