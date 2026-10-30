package com.dremioplatform.catalog.security;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Adapter implemented by the deployment's trusted OIDC/Numu authentication
 * integration. Principal names alone must never be converted into UUIDs.
 */
public interface CatalogIdentityVerifier {
    Uni<VerifiedCatalogIdentity> verify(SecurityContext context);
}
