package com.dremioplatform.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import tech.kayys.numou.foundation.domain.tenant.TenantId;
import tech.kayys.numou.security.domain.authorization.Principal;
import tech.kayys.numou.security.domain.identifier.PrincipalId;
import tech.kayys.numou.security.spi.port.TenantMembershipResolver;

/**
 * Optional Numu Security SPI adapter. The UUID must come from Toba's verified identity
 * resolver; this class does not authenticate credentials or trust request-supplied IDs.
 */
@ApplicationScoped
public class NumuTenantMembershipVerifier implements TenantMembershipVerifier {
    @Inject TenantMembershipResolver membershipResolver;

    @Override
    public Uni<Boolean> isActiveMember(UUID userId, UUID tenantId) {
        if (userId == null || tenantId == null) {
            return Uni.createFrom().item(false);
        }
        Principal principal = Principal.of(
                PrincipalId.of(userId), userId.toString(), TenantId.of(tenantId));
        return membershipResolver.isMember(principal, TenantId.of(tenantId))
                .onFailure().recoverWithItem(false)
                .map(Boolean.TRUE::equals);
    }
}
