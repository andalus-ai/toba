package com.dremioplatform.identity.service.dto;

import com.dremioplatform.identity.domain.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id, String name, String description, boolean systemRole,
        List<String> permissionKeys, Instant createdAt
) {
    public static RoleResponse from(Role r, List<String> permissionKeys) {
        return new RoleResponse(r.id, r.name, r.description, r.systemRole, permissionKeys, r.createdAt);
    }
}
