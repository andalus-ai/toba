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
    @Inject Instance<TenantMembershipVerifier> memberships;

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

    /** Check authoritative membership before granting access to a tenant. */
    public io.smallrye.mutiny.Uni<UUID> requireActiveMembership(
            ExternalIdentityResolver.VerifiedIdentity identity, UUID tenantId) {
        UUID userId = requireTenant(identity, tenantId);
        if (memberships.isUnsatisfied() || memberships.isAmbiguous()) {
            return io.smallrye.mutiny.Uni.createFrom().failure(
                    new WebApplicationException("Membership provider unavailable", 503));
        }
        return memberships.get().isActiveMember(userId, tenantId)
                .onFailure().recoverWithItem(false)
                .map(active -> {
                    if (!Boolean.TRUE.equals(active)) {
                        throw new WebApplicationException("Active tenant membership required", 403);
                    }
                    return userId;
                });
    }

    public UUID requireTenant(ExternalIdentityResolver.VerifiedIdentity resolved, UUID tenantId) {
        if (resolved == null || tenantId == null || !tenantId.equals(resolved.tenantId())) {
            throw new WebApplicationException("Tenant membership required", 403);
        }
        return resolved.userId();
    }
}
