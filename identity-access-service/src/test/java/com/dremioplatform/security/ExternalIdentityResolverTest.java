package com.dremioplatform.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExternalIdentityResolverTest {
    @Test void requiresCanonicalIdentifiers() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> new ExternalIdentityResolver.VerifiedIdentity(null, id, true));
        assertThrows(IllegalArgumentException.class,
                () -> new ExternalIdentityResolver.VerifiedIdentity(id, null, true));
    }

    @Test void preservesVerifiedTenant() {
        UUID user = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        var result = new ExternalIdentityResolver.VerifiedIdentity(user, tenant, true);
        assertEquals(user, result.userId());
        assertEquals(tenant, result.tenantId());
        assertTrue(result.active());
    }
}
