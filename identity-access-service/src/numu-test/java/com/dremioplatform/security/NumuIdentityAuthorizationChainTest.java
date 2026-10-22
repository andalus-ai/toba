package com.dremioplatform.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.WebApplicationException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.kayys.numou.security.spi.port.TenantMembershipResolver;

/**
 * In-process contract: verified identity -> Numu SPI adapter -> Toba tenant gate.
 * This deliberately uses a fake SPI; PostgreSQL-backed tests live in Numu.
 */
class NumuIdentityAuthorizationChainTest {
    private final UUID user = UUID.randomUUID();
    private final UUID tenant = UUID.randomUUID();
    private VerifiedIdentityBridge bridge;
    private TenantMembershipResolver resolver;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        resolver = mock(TenantMembershipResolver.class);
        var verifier = new NumuTenantMembershipVerifier();
        verifier.membershipResolver = resolver;

        Instance<ExternalIdentityResolver> providers = mock(Instance.class);
        Instance<TenantMembershipVerifier> memberships = mock(Instance.class);
        when(providers.isUnsatisfied()).thenReturn(false);
        when(providers.isAmbiguous()).thenReturn(false);
        when(providers.get()).thenReturn((subject, issuer) ->
                new ExternalIdentityResolver.VerifiedIdentity(user, tenant, true));
        when(memberships.isUnsatisfied()).thenReturn(false);
        when(memberships.isAmbiguous()).thenReturn(false);
        when(memberships.get()).thenReturn(verifier);

        bridge = new VerifiedIdentityBridge();
        bridge.providers = providers;
        bridge.memberships = memberships;
    }

    @Test
    void acceptsVerifiedActiveMember() {
        when(resolver.isMember(any(), any())).thenReturn(Uni.createFrom().item(true));
        var identity = bridge.resolve("trusted-subject", "trusted-issuer");
        assertEquals(user, bridge.requireActiveMembership(identity, tenant).await().indefinitely());
    }

    @Test
    void deniesInactiveMembership() {
        when(resolver.isMember(any(), any())).thenReturn(Uni.createFrom().item(false));
        var identity = bridge.resolve("trusted-subject", "trusted-issuer");
        assertStatus(403, () -> bridge.requireActiveMembership(identity, tenant).await().indefinitely());
    }

    @Test
    void deniesMismatchedTenantWithoutConsultingProvider() {
        var identity = bridge.resolve("trusted-subject", "trusted-issuer");
        assertStatus(403, () -> bridge.requireActiveMembership(identity, UUID.randomUUID()));
        verifyNoInteractions(resolver);
    }

    @Test
    void deniesProviderFailure() {
        when(resolver.isMember(any(), any())).thenReturn(
                Uni.createFrom().failure(new IllegalStateException("database unavailable")));
        var identity = bridge.resolve("trusted-subject", "trusted-issuer");
        assertStatus(403, () -> bridge.requireActiveMembership(identity, tenant).await().indefinitely());
    }

    @Test
    void deniesSynchronousProviderFailure() {
        when(resolver.isMember(any(), any())).thenThrow(new IllegalStateException("database unavailable"));
        var identity = bridge.resolve("trusted-subject", "trusted-issuer");
        assertStatus(403, () -> bridge.requireActiveMembership(identity, tenant).await().indefinitely());
    }


    @Test
    void rejectsMissingVerifiedCredentialsBeforeCallingProvider() {
        assertStatus(401, () -> bridge.resolve("", "trusted-issuer"));
        assertStatus(401, () -> bridge.resolve("trusted-subject", " "));
        verifyNoInteractions(resolver);
    }

    @Test
    void rejectsInactiveVerifiedIdentityBeforeMembershipCheck() {
        var identity = new ExternalIdentityResolver.VerifiedIdentity(user, tenant, false);
        assertStatus(403, () -> bridge.requireActiveMembership(identity, tenant));
        verifyNoInteractions(resolver);
    }

    @Test
    void rejectsNullCanonicalUserBeforeMembershipCheck() {
        var identity = new ExternalIdentityResolver.VerifiedIdentity(null, tenant, true);
        assertStatus(403, () -> bridge.requireActiveMembership(identity, tenant));
        verifyNoInteractions(resolver);
    }

    @Test
    void rejectsUnavailableMembershipProvider() {
        bridge.memberships = unavailableMemberships();
        var identity = bridge.resolve("trusted-subject", "trusted-issuer");
        assertStatus(503, () -> bridge.requireActiveMembership(identity, tenant).await().indefinitely());
        verifyNoInteractions(resolver);
    }

    @SuppressWarnings("unchecked")
    private static Instance<TenantMembershipVerifier> unavailableMemberships() {
        Instance<TenantMembershipVerifier> memberships = mock(Instance.class);
        when(memberships.isUnsatisfied()).thenReturn(true);
        return memberships;
    }

    private static void assertStatus(int status, Runnable action) {
        WebApplicationException failure = assertThrows(WebApplicationException.class, action::run);
        assertEquals(status, failure.getResponse().getStatus());
    }
}
