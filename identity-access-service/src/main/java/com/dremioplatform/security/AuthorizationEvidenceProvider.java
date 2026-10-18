package com.dremioplatform.security;

import java.util.Optional;
import java.util.UUID;

/** Must be implemented against authoritative membership, role and revocation state. */
public interface AuthorizationEvidenceProvider {
    /** Empty result means deny. Errors must not be interpreted as allow. */
    Optional<AuthorizationEvidence> resolve(UUID authenticatedPrincipalId, UUID tenantId,
                                            UUID workspaceId, String action, String resource);
}
