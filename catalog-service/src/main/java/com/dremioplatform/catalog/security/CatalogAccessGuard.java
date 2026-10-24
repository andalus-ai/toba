package com.dremioplatform.catalog.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.SecurityContext;
import java.util.UUID;

/**
 * Fail-closed catalog authorization. Only a trusted server-side provider can
 * supply the canonical actor; a principal name or HTTP header is insufficient.
 */
@ApplicationScoped
public class CatalogAccessGuard {
    @Inject Instance<CatalogAuthorizationProvider> providers;

    public Uni<UUID> authorize(SecurityContext context, String action, String resource) {
        if (context == null || context.getUserPrincipal() == null
                || context.getUserPrincipal().getName() == null
                || context.getUserPrincipal().getName().isBlank()
                || action == null || action.isBlank() || resource == null || resource.isBlank()
                || providers == null || providers.isUnsatisfied() || providers.isAmbiguous()) {
            return denied();
        }
        return Uni.createFrom().deferred(() -> {
            Uni<UUID> result = providers.get().authorize(context, action, resource);
            return result == null ? denied() : result;
        }).onItem().transform(actor -> {
            if (actor == null) throw new ForbiddenException("Catalog access denied");
            return actor;
        }).onFailure().transform(error -> new ForbiddenException("Catalog access denied"));
    }

    private Uni<UUID> denied() {
        return Uni.createFrom().failure(new ForbiddenException("Catalog access denied"));
    }
}
