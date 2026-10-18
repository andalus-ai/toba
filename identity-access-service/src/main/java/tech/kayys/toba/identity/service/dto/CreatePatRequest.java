package com.dremioplatform.identity.service.dto;

import com.dremioplatform.identity.domain.PrincipalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreatePatRequest(
        @NotNull PrincipalType principalType,
        @NotNull UUID principalId,
        @NotBlank String label,
        Instant expiresAt
) {
}
