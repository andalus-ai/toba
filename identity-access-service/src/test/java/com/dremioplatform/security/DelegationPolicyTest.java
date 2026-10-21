package com.dremioplatform.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DelegationPolicyTest {
    @Test void rejectsPrivilegeEscalation() {
        assertFalse(DelegationPolicy.canDelegate(Set.of("identity.user.read"), Set.of("identity.user.manage")));
    }
    @Test void permitsOnlyHeldPermissions() {
        assertTrue(DelegationPolicy.canDelegate(Set.of("read", "write"), Set.of("read")));
    }
    @Test void enforcesScopeSegmentBoundary() {
        assertTrue(DelegationPolicy.isWithinScope("tenant.acme", "tenant.acme.space.sales"));
        assertTrue(DelegationPolicy.isWithinScope("tenant.acme", "tenant.acme"));
        assertFalse(DelegationPolicy.isWithinScope("tenant.acme", "tenant.acmeevil"));
        assertFalse(DelegationPolicy.isWithinScope("tenant.acme", null));
    }
}
