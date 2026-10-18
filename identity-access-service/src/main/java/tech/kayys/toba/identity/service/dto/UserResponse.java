package com.dremioplatform.identity.service.dto;

import com.dremioplatform.identity.domain.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String displayName,
        User.UserStatus status,
        boolean federated,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.id, u.username, u.email, u.displayName, u.status,
                u.identityProviderId != null, u.createdAt, u.lastLoginAt);
    }
}
