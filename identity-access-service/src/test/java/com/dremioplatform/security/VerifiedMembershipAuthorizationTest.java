package com.dremioplatform.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerifiedMembershipAuthorizationTest {
    private final UUID user = UUID.randomUUID();
    private final UUID tenant = UUID.randomUUID();
    private VerifiedMembershipAuthorization authorization;
    private VerifiedIdentityBridge bridge;
    private ExternalIdentityResolver.VerifiedIdentity identity;

    @BeforeEach
    void setup() {
        bridge = mock(VerifiedIdentityBridge.class);
        authorization = new VerifiedMembershipAuthorization();
        authorization.identities = bridge;
        authorization.decisions = new AuthorizationDecisionService();
        identity = new ExternalIdentityResolver.VerifiedIdentity(user, tenant, true);
    }

    private boolean check(UUID requestedTenant, UUID workspace, String action,
                          Set<String> grants, UUID grantWorkspace) {
        return authorization.isAllowed(identity, requestedTenant, workspace, action,
                "catalog:orders", grants, grantWorkspace).await().indefinitely();
    }

    @Test
    void permitsActiveMemberWithExplicitActionGrant() {
        when(bridge.requireActiveMembership(identity, tenant)).thenReturn(Uni.createFrom().item(user));
        assertTrue(check(tenant, null, "catalog.read", Set.of("catalog.read"), null));
    }

    @Test
    void deniesMembershipWithoutActionGrant() {
        when(bridge.requireActiveMembership(identity, tenant)).thenReturn(Uni.createFrom().item(user));
        assertFalse(check(tenant, null, "catalog.read", Set.of("catalog.write"), null));
    }

    @Test
    void deniesWorkspaceScopeMismatch() {
        UUID workspace = UUID.randomUUID();
        when(bridge.requireActiveMembership(identity, tenant)).thenReturn(Uni.createFrom().item(user));
        assertFalse(check(tenant, workspace, "query.execute",
                Set.of("query.execute"), UUID.randomUUID()));
    }

    @Test
    void deniesMembershipProviderFailure() {
        when(bridge.requireActiveMembership(identity, tenant)).thenReturn(
                Uni.createFrom().failure(new IllegalStateException("unavailable")));
        assertFalse(check(tenant, null, "catalog.read", Set.of("catalog.read"), null));
    }

    @Test
    void deniesWrongTenantEvenWithActionGrant() {
        UUID wrongTenant = UUID.randomUUID();
        when(bridge.requireActiveMembership(identity, wrongTenant)).thenReturn(
                Uni.createFrom().failure(new IllegalArgumentException("wrong tenant")));
        assertFalse(check(wrongTenant, null, "catalog.read", Set.of("catalog.read"), null));
    }

    @Test
    void deniesMissingActionWithoutCallingMembership() {
        assertFalse(check(tenant, null, "", Set.of("catalog.read"), null));
        verifyNoInteractions(bridge);
    }
}
