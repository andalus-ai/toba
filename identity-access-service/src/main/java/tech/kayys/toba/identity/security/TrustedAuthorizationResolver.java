package com.dremioplatform.security;

import java.util.Optional;
import java.util.UUID;

/** Orchestrates evidence retrieval and the P1.5 decision core. */
public final class TrustedAuthorizationResolver {
    private final AuthorizationEvidenceProvider evidenceProvider;
    private final AuthorizationDecisionService decisions;

    public TrustedAuthorizationResolver(AuthorizationEvidenceProvider evidenceProvider,
                                        AuthorizationDecisionService decisions) {
        this.evidenceProvider = java.util.Objects.requireNonNull(evidenceProvider);
        this.decisions = java.util.Objects.requireNonNull(decisions);
    }

    public boolean isAllowed(UUID authenticatedPrincipalId, String action, String resource,
                             UUID tenantId, UUID workspaceId) {
        if (authenticatedPrincipalId == null || tenantId == null || action == null
                || action.isBlank() || resource == null || resource.isBlank()) return false;
        try {
            Optional<AuthorizationEvidence> result = evidenceProvider.resolve(
                authenticatedPrincipalId, tenantId, workspaceId, action, resource);
            if (result == null || result.isEmpty()) return false;
            AuthorizationEvidence evidence = result.get();
            if (!evidence.active() || !authenticatedPrincipalId.equals(evidence.principalId())) return false;
            return decisions.isAllowed(new AuthorizationRequest(
                authenticatedPrincipalId, action, resource, tenantId, workspaceId,
                evidence.grantedActions(), evidence.tenantId(), evidence.workspaceId()));
        } catch (RuntimeException unavailable) {
            // Do not allow access when the policy store or resolver fails.
            return false;
        }
    }
}
