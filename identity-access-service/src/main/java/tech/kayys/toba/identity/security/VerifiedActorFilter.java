package com.dremioplatform.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;

/** Compatibility boundary until REST resources accept a verified actor directly. */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class VerifiedActorFilter implements ContainerRequestFilter {
    @Inject SecurityIdentity identity;

    @Override
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath();
        if (!(path.equals("api/v1") || path.startsWith("api/v1/"))) return;
        // Login cannot have a previously issued platform principal.
        if (path.equals("api/v1/auth/login")) return;
        if (identity.isAnonymous()) throw new WebApplicationException("Authentication required", 401);

        // Use the authenticated identity, never a caller-provided actor ID.
        // Reject non-UUID principals rather than guessing a user mapping.
        String actor = canonicalActor(identity.getPrincipal().getName());
        String supplied = request.getHeaderString("X-Actor-Id");
        if (supplied != null && !actor.equals(supplied)) {
            throw new WebApplicationException("Actor does not match authenticated principal", 403);
        }
        request.getHeaders().putSingle("X-Actor-Id", actor);
    }

    static String canonicalActor(String subject) {
        try {
            return UUID.fromString(subject).toString();
        } catch (RuntimeException ex) {
            throw new WebApplicationException("Authenticated principal must be a UUID", 403);
        }
    }
}
