package com.dremioplatform.security;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UnconfiguredAuthorizationEvidenceProviderTest {
    @Test
    void defaultProviderNeverReturnsGrants() {
        UUID principal = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        var provider = new UnconfiguredAuthorizationEvidenceProvider();
        assertFalse(provider.resolve(principal, tenant, null, "catalog.dataset.read", "dataset:orders").isPresent());
        assertFalse(new TrustedAuthorizationResolver(provider, new AuthorizationDecisionService())
            .isAllowed(principal, "catalog.dataset.read", "dataset:orders", tenant, null));
    }
}
