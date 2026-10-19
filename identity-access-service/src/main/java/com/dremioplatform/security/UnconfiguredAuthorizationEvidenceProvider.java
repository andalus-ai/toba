package com.dremioplatform.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

/** Production-safe fallback: never invent grants when no authoritative store is wired. */
@ApplicationScoped
public class UnconfiguredAuthorizationEvidenceProvider implements AuthorizationEvidenceProvider {
    @Override
    public Optional<AuthorizationEvidence> resolve(UUID authenticatedPrincipalId,
            UUID tenantId, UUID workspaceId, String action, String resource) {
        return Optional.empty();
    }
}
