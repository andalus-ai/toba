package com.dremioplatform.ai.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AiFunctionRequest(
        @NotBlank String inputText,
        /** Only used by AI_CLASSIFY — the candidate labels to choose from. */
        List<String> categories,
        String sourceDatasetPath
) {
}
