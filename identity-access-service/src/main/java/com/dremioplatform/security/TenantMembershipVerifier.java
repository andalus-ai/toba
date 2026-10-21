package com.dremioplatform.security;

import io.smallrye.mutiny.Uni;
import java.util.UUID;

/** Adapter port for Numu TenantMembershipPort or another trusted membership authority. */
public interface TenantMembershipVerifier {
    Uni<Boolean> isActiveMember(UUID userId, UUID tenantId);
}
