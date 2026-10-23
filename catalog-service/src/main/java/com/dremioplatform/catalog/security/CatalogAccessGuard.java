package com.dremioplatform.catalog.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.SecurityContext;
import java.util.UUID;

/**
 * Fail-closed catalog boundary until a trusted identity + membership + grants
 * implementation is installed. HTTP actor headers are never credentials.
 */
@ApplicationScoped
public class CatalogAccessGuard {
    /**
     * Never infer the actor from a caller-controlled header or an arbitrary
     * principal name. Until a verified canonical identity adapter is installed,
     * catalog mutations must fail closed.
     */
    public UUID requireActorId(SecurityContext context) {
        throw new ForbiddenException("Verified canonical actor unavailable");
    }

    public void require(SecurityContext context, String action, String resource) {
        if (context == null || context.getUserPrincipal() == null
                || context.getUserPrincipal().getName() == null
                || context.getUserPrincipal().getName().isBlank()
                || action == null || action.isBlank()
                || resource == null || resource.isBlank()) {
            throw new ForbiddenException("Catalog access denied");
        }
        // Authentication alone is insufficient. A future integration must
        // resolve canonical tenant membership and authoritative Toba grants.
        throw new ForbiddenException("Catalog authorization provider not configured");
    }
}
