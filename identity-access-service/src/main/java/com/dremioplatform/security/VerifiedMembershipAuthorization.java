package com.dremioplatform.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

/**
 * Combines authoritative tenant membership with server-side Toba grants.
 * Callers cannot supply grant sets or membership evidence.
 */
@ApplicationScoped
public class VerifiedMembershipAuthorization {
    @Inject VerifiedIdentityBridge identities;
    @Inject TrustedAuthorizationResolver grants;

    public Uni<Boolean> isAllowed(
            ExternalIdentityResolver.VerifiedIdentity identity,
            UUID tenantId,
            UUID workspaceId,
            String action,
            String resource) {
        if (identity == null || tenantId == null || action == null || action.isBlank()
                || resource == null || resource.isBlank()) {
            return Uni.createFrom().item(false);
        }
        return Uni.createFrom().deferred(() -> identities.requireActiveMembership(identity, tenantId))
                .map(userId -> Boolean.TRUE.equals(grants.isAllowed(
                        userId, action, resource, tenantId, workspaceId)))
                .onFailure().recoverWithItem(false);
    }
}
