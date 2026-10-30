package com.dremioplatform.catalog.security;

import io.smallrye.mutiny.Uni;

/**
 * Deployment adapter for authoritative tenant membership and Toba action
 * grants. Return true only if BOTH checks pass for the supplied identity,
 * tenant/workspace scope, action and resource. Revocation must deny.
 */
public interface CatalogMembershipGrantsVerifier {
    Uni<Boolean> isAllowed(VerifiedCatalogIdentity identity, String action, String resource);
}
