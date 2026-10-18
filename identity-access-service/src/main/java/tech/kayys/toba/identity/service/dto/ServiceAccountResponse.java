package com.dremioplatform.identity.service.dto;

import com.dremioplatform.identity.domain.ServiceAccount;
import java.time.Instant;
import java.util.UUID;

public record ServiceAccountResponse(
        UUID id, String name, String description, UUID ownerUserId, boolean enabled, Instant createdAt
) {
    public static ServiceAccountResponse from(ServiceAccount sa) {
        return new ServiceAccountResponse(sa.id, sa.name, sa.description, sa.ownerUserId, sa.enabled, sa.createdAt);
    }
}
