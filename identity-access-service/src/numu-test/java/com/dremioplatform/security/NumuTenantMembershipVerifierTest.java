package com.dremioplatform.security;

import static org.junit.jupiter.api.Assertions.*;

import io.smallrye.mutiny.Uni;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import tech.kayys.numou.foundation.domain.tenant.TenantId;
import tech.kayys.numou.security.domain.authorization.Principal;
import tech.kayys.numou.security.spi.port.TenantMembershipResolver;

/** Contract tests for the optional Numu SPI adapter; run only with the Numu profile enabled. */
class NumuTenantMembershipVerifierTest {

    @Test
    void delegatesCanonicalIdentityAndTenantToAuthoritativeResolver() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();
        TenantMembershipResolver resolver = (principal, tenant) -> {
            calls.incrementAndGet();
            assertNotNull(principal);
            assertEquals(TenantId.of(tenantId), tenant);
            return Uni.createFrom().item(true);
        };
        NumuTenantMembershipVerifier verifier = new NumuTenantMembershipVerifier();
        verifier.membershipResolver = resolver;

        assertTrue(verifier.isActiveMember(userId, tenantId).await().indefinitely());
        assertEquals(1, calls.get());
    }

    @Test
    void returnsFalseWhenMembershipIsNotActive() {
        TenantMembershipResolver resolver =
                (principal, tenant) -> Uni.createFrom().item(false);
        NumuTenantMembershipVerifier verifier = new NumuTenantMembershipVerifier();
        verifier.membershipResolver = resolver;

        assertFalse(verifier.isActiveMember(UUID.randomUUID(), UUID.randomUUID())
                .await().indefinitely());
    }

    @Test
    void failsClosedWhenMembershipProviderFails() {
        TenantMembershipResolver resolver = (principal, tenant) ->
                Uni.createFrom().failure(new IllegalStateException("membership store unavailable"));
        NumuTenantMembershipVerifier verifier = new NumuTenantMembershipVerifier();
        verifier.membershipResolver = resolver;

        assertFalse(verifier.isActiveMember(UUID.randomUUID(), UUID.randomUUID())
                .await().indefinitely());
    }

    @Test
    void failsClosedWhenResolverThrowsSynchronously() {
        TenantMembershipResolver resolver = (principal, tenant) -> {
            throw new IllegalStateException("resolver disconnected");
        };
        NumuTenantMembershipVerifier verifier = new NumuTenantMembershipVerifier();
        verifier.membershipResolver = resolver;

        assertFalse(verifier.isActiveMember(UUID.randomUUID(), UUID.randomUUID())
                .await().indefinitely());
    }

    @Test
    void rejectsNullIdentifiersWithoutCallingProvider() {
        AtomicInteger calls = new AtomicInteger();
        TenantMembershipResolver resolver = (principal, tenant) -> {
            calls.incrementAndGet();
            return Uni.createFrom().item(true);
        };
        NumuTenantMembershipVerifier verifier = new NumuTenantMembershipVerifier();
        verifier.membershipResolver = resolver;

        assertFalse(verifier.isActiveMember(null, UUID.randomUUID()).await().indefinitely());
        assertFalse(verifier.isActiveMember(UUID.randomUUID(), null).await().indefinitely());
        assertEquals(0, calls.get());
    }
}
