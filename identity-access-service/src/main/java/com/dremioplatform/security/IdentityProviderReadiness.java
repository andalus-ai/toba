package com.dremioplatform.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Optional strict startup gate for installations using external identity.
 * This does not authenticate requests or replace runtime membership checks.
 */
@ApplicationScoped
public class IdentityProviderReadiness {
    @Inject Instance<ExternalIdentityResolver> identityResolvers;
    @Inject Instance<TenantMembershipVerifier> membershipVerifiers;

    @ConfigProperty(name = "toba.identity.require-providers-at-startup", defaultValue = "false")
    boolean requireProvidersAtStartup;

    void validateAtStartup(@Observes StartupEvent event) {
        if (requireProvidersAtStartup) {
            validateProviders();
        }
    }

    void validateProviders() {
        if (identityResolvers.isUnsatisfied() || identityResolvers.isAmbiguous()) {
            throw new IllegalStateException("Exactly one external identity resolver must be available");
        }
        if (membershipVerifiers.isUnsatisfied() || membershipVerifiers.isAmbiguous()) {
            throw new IllegalStateException("Exactly one tenant membership verifier must be available");
        }
    }
}
