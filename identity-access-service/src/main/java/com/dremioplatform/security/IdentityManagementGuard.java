package com.dremioplatform.security;

import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.service.RoleAssignmentService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Deny-by-default identity administration authorization. */
@ApplicationScoped
public class IdentityManagementGuard {
    @Inject SecurityIdentity identity;
    @Inject RoleAssignmentService assignments;
    @ConfigProperty(name = "toba.identity.management-scope-path")
    java.util.Optional<String> configuredScope;

    public UUID actor() {
        if (identity.isAnonymous()) throw new WebApplicationException("Authentication required", 401);
        return UUID.fromString(VerifiedActorFilter.canonicalActor(identity.getPrincipal().getName()));
    }

    public String scope() {
        String value = configuredScope.orElse("").trim();
        if (value.isEmpty()) throw new WebApplicationException("Management scope not configured", 503);
        return value;
    }

    public Uni<UUID> require(String permission) {
        UUID actor = actor();
        return assignments.resolveEffectivePermissions(PrincipalType.USER, actor, scope())
                .onFailure().recoverWithItem(Set.of())
                .map(granted -> {
                    if (!granted.contains(permission)) throw new WebApplicationException("Forbidden", 403);
                    return actor;
                });
    }

    public Uni<UUID> requireWithinScope(String permission, String requestedScope) {
        String root = scope();
        if (!DelegationPolicy.isWithinScope(root, requestedScope)) {
            throw new WebApplicationException("Scope outside management boundary", 403);
        }
        return require(permission);
    }
}
