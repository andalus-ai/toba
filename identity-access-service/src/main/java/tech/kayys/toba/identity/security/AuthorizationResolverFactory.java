package com.dremioplatform.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/** CDI wiring for the authorization resolver; no grant can be issued by default. */
@ApplicationScoped
public class AuthorizationResolverFactory {
    @Inject AuthorizationEvidenceProvider evidenceProvider;
    @Inject AuthorizationDecisionService decisions;

    @Produces
    @ApplicationScoped
    public TrustedAuthorizationResolver resolver() {
        return new TrustedAuthorizationResolver(evidenceProvider, decisions);
    }
}
