package com.dremioplatform.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import tech.kayys.numou.identity.application.port.TenantMembershipPort;
import tech.kayys.numou.identity.application.security.Principal;
import tech.kayys.numou.identity.domain.user.UserId;

/**
 * Optional adapter compiled only with -Pnumu-identity-integration.
 * Inputs must already be resolved from verified authentication, not request headers.
 */
@ApplicationScoped
public class NumuTenantMembershipVerifier implements TenantMembershipVerifier {
    @Inject TenantMembershipPort membershipPort;

    @Override
    public Uni<Boolean> isActiveMember(UUID userId, UUID tenantId) {
        if (userId == null || tenantId == null) {
            return Uni.createFrom().item(false);
        }
        Principal verifiedPrincipal = new Principal(
                UserId.of(userId), userId.toString(), null, null, null, null);
        return membershipPort.isMember(verifiedPrincipal, tenantId.toString())
                .onFailure().recoverWithItem(false)
                .map(Boolean.TRUE::equals);
    }
}
