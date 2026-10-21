package com.dremioplatform.security;

import java.util.UUID;

/**
 * Stable boundary for shared identity providers (Numu, OIDC, or standalone).
 * Implementations must derive identity from verified credentials, never headers.
 */
public interface ExternalIdentityResolver {
    VerifiedIdentity resolve(String verifiedSubject, String issuer);

    /** Tenant membership must be established by a trusted provider, not a request parameter. */
    record VerifiedIdentity(UUID userId, UUID tenantId, boolean active) {
        public VerifiedIdentity {
            if (userId == null || tenantId == null) {
                throw new IllegalArgumentException("Verified identity requires user and tenant");
            }
        }
    }
}
