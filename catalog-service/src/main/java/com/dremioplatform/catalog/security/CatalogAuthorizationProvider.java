package com.dremioplatform.catalog.security;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.SecurityContext;
import java.util.UUID;

/**
 * Adapter contract for trusted, server-side catalog authorization.
 * Implementations must validate credentials, canonical tenant membership,
 * workspace scope and authoritative action grants before returning an actor.
 * Never accept actor IDs, grants or tenant membership from HTTP headers.
 */
public interface CatalogAuthorizationProvider {
    Uni<UUID> authorize(SecurityContext securityContext, String action, String resource);
}
