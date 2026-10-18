package com.dremioplatform.identity.service.dto;

import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.domain.RoleScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record GrantRoleAssignmentRequest(
        @NotNull PrincipalType principalType,
        @NotNull UUID principalId,
        @NotNull UUID roleId,
        @NotNull RoleScopeType scopeType,
        @NotBlank String scopePath,
        Instant expiresAt
) {
}
