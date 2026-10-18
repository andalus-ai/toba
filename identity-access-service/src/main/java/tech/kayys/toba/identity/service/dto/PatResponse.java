package com.dremioplatform.identity.service.dto;

import com.dremioplatform.identity.domain.PersonalAccessToken;
import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.domain.TokenStatus;
import java.time.Instant;
import java.util.UUID;

public record PatResponse(
        UUID id, PrincipalType principalType, UUID principalId, String label,
        String displayPrefix, TokenStatus status, Instant createdAt, Instant expiresAt, Instant lastUsedAt
) {
    public static PatResponse from(PersonalAccessToken pat) {
        return new PatResponse(pat.id, pat.principalType, pat.principalId, pat.label,
                pat.displayPrefix, pat.status, pat.createdAt, pat.expiresAt, pat.lastUsedAt);
    }
}
