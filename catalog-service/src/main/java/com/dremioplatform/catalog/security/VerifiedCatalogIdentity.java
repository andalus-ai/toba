package com.dremioplatform.catalog.security;

import java.util.UUID;

/**
 * Canonical identity derived exclusively from cryptographically validated
 * credentials by a trusted authentication adapter, never request headers.
 */
public record VerifiedCatalogIdentity(UUID userId, UUID tenantId, UUID workspaceId,
                                      String subject, String issuer) {
    public VerifiedCatalogIdentity {
        if (userId == null || tenantId == null || subject == null || subject.isBlank()
                || issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("Verified catalog identity is incomplete");
        }
    }
}
