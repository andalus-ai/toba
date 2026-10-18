package com.dremioplatform.security;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthorizationDecisionServiceTest {
    private final AuthorizationDecisionService decisions = new AuthorizationDecisionService();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID WORKSPACE = UUID.randomUUID();

    private AuthorizationRequest request(UUID requestedTenant, UUID requestedWorkspace, Set<String> grants) {
        return new AuthorizationRequest(USER, "catalog.dataset.read", "dataset:orders",
            requestedTenant, requestedWorkspace, grants, TENANT, WORKSPACE);
    }

    @Test void allowsMatchingScopeAndExplicitAction() {
        assertTrue(decisions.isAllowed(request(TENANT, WORKSPACE, Set.of("catalog.dataset.read"))));
    }

    @Test void rejectsDifferentTenant() {
        assertFalse(decisions.isAllowed(request(UUID.randomUUID(), WORKSPACE, Set.of("catalog.dataset.read"))));
    }

    @Test void rejectsDifferentWorkspace() {
        assertFalse(decisions.isAllowed(request(TENANT, UUID.randomUUID(), Set.of("catalog.dataset.read"))));
    }

    @Test void rejectsMissingGrant() {
        assertFalse(decisions.isAllowed(request(TENANT, WORKSPACE, Set.of("catalog.dataset.write"))));
    }

    @Test void rejectsWildcardGrant() {
        assertFalse(decisions.isAllowed(request(TENANT, WORKSPACE, Set.of("*"))));
    }

    @Test void rejectsUnresolvedPrincipalAndMembership() {
        assertFalse(decisions.isAllowed(null));
        assertFalse(decisions.isAllowed(new AuthorizationRequest(null, "catalog.dataset.read",
            "dataset:orders", TENANT, WORKSPACE, Set.of("catalog.dataset.read"), TENANT, WORKSPACE)));
        assertFalse(decisions.isAllowed(new AuthorizationRequest(USER, "catalog.dataset.read",
            "dataset:orders", TENANT, WORKSPACE, Set.of("catalog.dataset.read"), null, WORKSPACE)));
    }
}
