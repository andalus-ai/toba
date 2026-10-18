package com.dremioplatform.identity.service.dto;

import java.time.Instant;
import java.util.UUID;

/** The only place the raw token value ever appears. Not persisted, not logged. */
public record CreatePatResponse(
        UUID id, String label, String rawToken, String displayPrefix, Instant expiresAt
) {
}
