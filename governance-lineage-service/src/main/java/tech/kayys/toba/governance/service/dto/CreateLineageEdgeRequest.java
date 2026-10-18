package com.dremioplatform.governance.service.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLineageEdgeRequest(
        @NotBlank String sourcePath,
        String sourceColumn,
        @NotBlank String targetPath,
        String targetColumn,
        String transformationType,
        String sourceJobId
) {
}
