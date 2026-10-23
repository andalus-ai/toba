package com.dremioplatform.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side composition of tenant membership and Toba action grants.
 * The caller must supply an identity resolved from validated credentials.
 * Grant evidence must originate from a trusted server-side authorization source.
 */
@ApplicationScoped
public class VerifiedMembershipAuthorization {
    @Inject VerifiedIdentityBridge identities;
    @Inject AuthorizationDecisionService decisions;

    public Uni<Boolean> isAllowed(
            ExternalIdentityResolver.VerifiedIdentity identity,
            UUID tenantId,
            UUID workspaceId,
            String action,
            String resource,
            Set<String> trustedGrants,
            UUID grantWorkspaceId) {
        if (identity == null || tenantId == null || action == null || action.isBlank()
                || resource == null || resource.isBlank()) {
            return Uni.createFrom().item(false);
        }
        return Uni.createFrom().deferred(() -> identities.requireActiveMembership(identity, tenantId))
                .map(userId -> decisions.isAllowed(new AuthorizationRequest(
                        userId, action, resource, tenantId, workspaceId,
                        trustedGrants, tenantId, grantWorkspaceId)))
                .onFailure().recoverWithItem(false);
    }
}
