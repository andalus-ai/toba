package com.dremioplatform.identity.service.dto;

import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.domain.RoleAssignment;
import com.dremioplatform.identity.domain.RoleScopeType;
import java.time.Instant;
import java.util.UUID;

public record RoleAssignmentResponse(
        UUID id, PrincipalType principalType, UUID principalId, UUID roleId,
        RoleScopeType scopeType, String scopePath, Instant grantedAt, Instant expiresAt
) {
    public static RoleAssignmentResponse from(RoleAssignment ra) {
        return new RoleAssignmentResponse(
                ra.id, ra.principalType, ra.principalId, ra.role.id,
                ra.scopeType, ra.scopePath, ra.grantedAt, ra.expiresAt);
    }
}
