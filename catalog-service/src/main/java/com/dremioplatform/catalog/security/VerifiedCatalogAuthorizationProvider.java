package com.dremioplatform.catalog.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.SecurityContext;
import java.util.UUID;

/**
 * Composes credential verification and authoritative membership/grant checks.
 * Missing, ambiguous or failing adapters always deny; no local fallback.
 */
@ApplicationScoped
public class VerifiedCatalogAuthorizationProvider implements CatalogAuthorizationProvider {
    @Inject Instance<CatalogIdentityVerifier> identities;
    @Inject Instance<CatalogMembershipGrantsVerifier> permissions;

    @Override
    public Uni<UUID> authorize(SecurityContext context, String action, String resource) {
        if (context == null || action == null || action.isBlank()
                || resource == null || resource.isBlank()
                || identities == null || identities.isUnsatisfied() || identities.isAmbiguous()
                || permissions == null || permissions.isUnsatisfied() || permissions.isAmbiguous()) {
            return denied();
        }
        return Uni.createFrom().deferred(() -> {
            Uni<VerifiedCatalogIdentity> identity = identities.get().verify(context);
            return identity == null ? Uni.createFrom().<VerifiedCatalogIdentity>nullItem() : identity;
        }).onItem().transformToUni(identity -> {
            if (identity == null) return denied();
            return Uni.createFrom().deferred(() -> {
                Uni<Boolean> allowed = permissions.get().isAllowed(identity, action, resource);
                return allowed == null ? Uni.createFrom().<Boolean>nullItem() : allowed;
            }).onItem().transformToUni(allowed ->
                    Boolean.TRUE.equals(allowed) ? Uni.createFrom().item(identity.userId()) : denied());
        }).onFailure().transform(error -> new ForbiddenException("Catalog access denied"));
    }

    private <T> Uni<T> denied() {
        return Uni.createFrom().failure(new ForbiddenException("Catalog access denied"));
    }
}
