package com.dremioplatform.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityProviderReadinessTest {
    private IdentityProviderReadiness readiness;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        readiness = new IdentityProviderReadiness();
        readiness.identityResolvers = mock(Instance.class);
        readiness.membershipVerifiers = mock(Instance.class);
    }

    @Test
    void acceptsExactlyOneOfEachProvider() {
        assertDoesNotThrow(readiness::validateProviders);
    }

    @Test
    void rejectsMissingIdentityProvider() {
        when(readiness.identityResolvers.isUnsatisfied()).thenReturn(true);
        assertThrows(IllegalStateException.class, readiness::validateProviders);
    }

    @Test
    void rejectsAmbiguousIdentityProvider() {
        when(readiness.identityResolvers.isAmbiguous()).thenReturn(true);
        assertThrows(IllegalStateException.class, readiness::validateProviders);
    }

    @Test
    void rejectsMissingMembershipProvider() {
        when(readiness.membershipVerifiers.isUnsatisfied()).thenReturn(true);
        assertThrows(IllegalStateException.class, readiness::validateProviders);
    }

    @Test
    void rejectsAmbiguousMembershipProvider() {
        when(readiness.membershipVerifiers.isAmbiguous()).thenReturn(true);
        assertThrows(IllegalStateException.class, readiness::validateProviders);
    }

    @Test
    void optInStartupGateDoesNotAffectStandaloneMode() {
        readiness.requireProvidersAtStartup = false;
        when(readiness.identityResolvers.isUnsatisfied()).thenReturn(true);
        assertDoesNotThrow(() -> readiness.validateAtStartup(null));
    }

    @Test
    void enabledStartupGateRejectsMissingProviders() {
        readiness.requireProvidersAtStartup = true;
        when(readiness.identityResolvers.isUnsatisfied()).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> readiness.validateAtStartup(null));
    }
}
