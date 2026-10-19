package com.dremioplatform.security;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
class AuthorizationResolverWiringTest {
    @Inject TrustedAuthorizationResolver resolver;

    @Test
    void injectedResolverDeniesWithoutAuthoritativeEvidence() {
        assertFalse(resolver.isAllowed(UUID.randomUUID(), "catalog.dataset.read",
            "dataset:orders", UUID.randomUUID(), null));
    }
}
