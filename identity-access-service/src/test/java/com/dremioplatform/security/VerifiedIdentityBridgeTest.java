package com.dremioplatform.security;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.ws.rs.WebApplicationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerifiedIdentityBridgeTest {
    @Test void rejectsWrongTenant() {
        var bridge = new VerifiedIdentityBridge();
        var identity = new ExternalIdentityResolver.VerifiedIdentity(
                UUID.randomUUID(), UUID.randomUUID(), true);
        assertEquals(403, assertThrows(WebApplicationException.class,
                () -> bridge.requireTenant(identity, UUID.randomUUID())).getResponse().getStatus());
    }

    @Test void acceptsMatchingTenant() {
        var bridge = new VerifiedIdentityBridge();
        UUID user = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        assertEquals(user, bridge.requireTenant(
                new ExternalIdentityResolver.VerifiedIdentity(user, tenant, true), tenant));
    }

    @Test void rejectsMissingTenant() {
        var bridge = new VerifiedIdentityBridge();
        assertEquals(403, assertThrows(WebApplicationException.class,
                () -> bridge.requireTenant(null, UUID.randomUUID())).getResponse().getStatus());
    }
    @Test void rejectsInactiveIdentityEvenWithMatchingTenant() {
        var bridge = new VerifiedIdentityBridge();
        UUID tenant = UUID.randomUUID();
        var identity = new ExternalIdentityResolver.VerifiedIdentity(UUID.randomUUID(), tenant, false);
        assertEquals(403, assertThrows(WebApplicationException.class,
                () -> bridge.requireTenant(identity, tenant)).getResponse().getStatus());
    }

    @Test void rejectsIdentityWithMissingUser() {
        var bridge = new VerifiedIdentityBridge();
        UUID tenant = UUID.randomUUID();
        var identity = new ExternalIdentityResolver.VerifiedIdentity(null, tenant, true);
        assertEquals(403, assertThrows(WebApplicationException.class,
                () -> bridge.requireTenant(identity, tenant)).getResponse().getStatus());
    }

    @Test void rejectsNullRequestedTenant() {
        var bridge = new VerifiedIdentityBridge();
        var identity = new ExternalIdentityResolver.VerifiedIdentity(UUID.randomUUID(), UUID.randomUUID(), true);
        assertEquals(403, assertThrows(WebApplicationException.class,
                () -> bridge.requireTenant(identity, null)).getResponse().getStatus());
    }

}
