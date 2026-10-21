package com.dremioplatform.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.UUID;

/** Maps a verified provider identity to Toba's canonical user and tenant. */
@ApplicationScoped
public class VerifiedIdentityBridge {
    @Inject Instance<ExternalIdentityResolver> providers;

    public ExternalIdentityResolver.VerifiedIdentity resolve(String subject, String issuer) {
        if (subject == null || subject.isBlank() || issuer == null || issuer.isBlank()) {
            throw new WebApplicationException("Verified subject and issuer required", 401);
        }
        if (providers.isUnsatisfied() || providers.isAmbiguous()) {
            throw new WebApplicationException("Identity provider unavailable", 503);
        }
        ExternalIdentityResolver.VerifiedIdentity resolved;
        try {
            resolved = providers.get().resolve(subject, issuer);
        } catch (RuntimeException exception) {
            throw new WebApplicationException("Identity resolution rejected", 403);
        }
        if (resolved == null || !resolved.active()) {
            throw new WebApplicationException("Identity inactive", 403);
        }
        return resolved;
    }

    public UUID requireTenant(ExternalIdentityResolver.VerifiedIdentity resolved, UUID tenantId) {
        if (resolved == null || tenantId == null || !tenantId.equals(resolved.tenantId())) {
            throw new WebApplicationException("Tenant membership required", 403);
        }
        return resolved.userId();
    }
}
