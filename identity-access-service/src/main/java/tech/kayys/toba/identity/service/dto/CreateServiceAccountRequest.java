package com.dremioplatform.identity.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateServiceAccountRequest(
        @NotBlank @Size(min = 3, max = 255) String name,
        String description,
        @NotBlank UUID ownerUserId
) {
}
