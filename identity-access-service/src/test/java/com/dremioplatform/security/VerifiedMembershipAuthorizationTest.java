package com.dremioplatform.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerifiedMembershipAuthorizationTest {
    private final UUID user = UUID.randomUUID();
    private final UUID tenant = UUID.randomUUID();
    private VerifiedMembershipAuthorization authorization;
    private VerifiedIdentityBridge membership;
    private TrustedAuthorizationResolver grants;
    private ExternalIdentityResolver.VerifiedIdentity identity;

    @BeforeEach
    void setup() {
        membership = mock(VerifiedIdentityBridge.class);
        grants = mock(TrustedAuthorizationResolver.class);
        authorization = new VerifiedMembershipAuthorization();
        authorization.identities = membership;
        authorization.grants = grants;
        identity = new ExternalIdentityResolver.VerifiedIdentity(user, tenant, true);
    }

    private boolean check(UUID requestedTenant, UUID workspace, String action) {
        return authorization.isAllowed(identity, requestedTenant, workspace,
                action, "catalog:orders").await().indefinitely();
    }

    @Test
    void allowsOnlyWhenMembershipAndAuthoritativeGrantAgree() {
        when(membership.requireActiveMembership(identity, tenant)).thenReturn(Uni.createFrom().item(user));
        when(grants.isAllowed(user, "catalog.read", "catalog:orders", tenant, null)).thenReturn(true);
        assertTrue(check(tenant, null, "catalog.read"));
        verify(grants).isAllowed(user, "catalog.read", "catalog:orders", tenant, null);
    }

    @Test
    void deniesActiveMemberWithoutServerSideGrant() {
        when(membership.requireActiveMembership(identity, tenant)).thenReturn(Uni.createFrom().item(user));
        assertFalse(check(tenant, null, "catalog.read"));
    }

    @Test
    void deniesMembershipFailureBeforeConsultingGrants() {
        when(membership.requireActiveMembership(identity, tenant)).thenReturn(
                Uni.createFrom().failure(new IllegalStateException("membership unavailable")));
        assertFalse(check(tenant, null, "catalog.read"));
        verifyNoInteractions(grants);
    }

    @Test
    void deniesGrantResolverFailure() {
        when(membership.requireActiveMembership(identity, tenant)).thenReturn(Uni.createFrom().item(user));
        when(grants.isAllowed(user, "catalog.read", "catalog:orders", tenant, null))
                .thenThrow(new IllegalStateException("policy store unavailable"));
        assertFalse(check(tenant, null, "catalog.read"));
    }

    @Test
    void deniesInvalidActionBeforeMembershipLookup() {
        assertFalse(check(tenant, null, " "));
        verifyNoInteractions(membership, grants);
    }

    @Test
    void passesWorkspaceToAuthoritativeResolver() {
        UUID workspace = UUID.randomUUID();
        when(membership.requireActiveMembership(identity, tenant)).thenReturn(Uni.createFrom().item(user));
        when(grants.isAllowed(user, "query.execute", "catalog:orders", tenant, workspace)).thenReturn(true);
        assertTrue(check(tenant, workspace, "query.execute"));
        verify(grants).isAllowed(user, "query.execute", "catalog:orders", tenant, workspace);
    }
}
