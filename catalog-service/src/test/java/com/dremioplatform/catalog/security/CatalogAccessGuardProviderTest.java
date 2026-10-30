package com.dremioplatform.catalog.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogAccessGuardProviderTest {
    private CatalogAccessGuard guard;
    private Instance<CatalogAuthorizationProvider> providers;
    private CatalogAuthorizationProvider provider;
    private SecurityContext context;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        guard = new CatalogAccessGuard();
        providers = mock(Instance.class);
        provider = mock(CatalogAuthorizationProvider.class);
        context = mock(SecurityContext.class);
        when(context.getUserPrincipal()).thenReturn((Principal) () -> "verified-principal");
        when(providers.get()).thenReturn(provider);
        guard.providers = providers;
    }

    private UUID authorize() {
        return guard.authorize(context, "catalog.table.read", "sales/orders").await().indefinitely();
    }

    @Test void acceptsOnlyCanonicalActorFromProvider() {
        UUID actor = UUID.randomUUID();
        when(provider.authorize(context, "catalog.table.read", "sales/orders"))
                .thenReturn(Uni.createFrom().item(actor));
        assertEquals(actor, authorize());
    }

    @Test void deniesMissingProvider() {
        when(providers.isUnsatisfied()).thenReturn(true);
        assertThrows(ForbiddenException.class, this::authorize);
        verifyNoInteractions(provider);
    }

    @Test void deniesAmbiguousProviders() {
        when(providers.isAmbiguous()).thenReturn(true);
        assertThrows(ForbiddenException.class, this::authorize);
        verifyNoInteractions(provider);
    }

    @Test void deniesNullProviderResult() {
        assertThrows(ForbiddenException.class, this::authorize);
    }

    @Test void deniesNullActor() {
        when(provider.authorize(context, "catalog.table.read", "sales/orders"))
                .thenReturn(Uni.createFrom().nullItem());
        assertThrows(ForbiddenException.class, this::authorize);
    }

    @Test void deniesProviderFailure() {
        when(provider.authorize(context, "catalog.table.read", "sales/orders"))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("provider unavailable")));
        assertThrows(ForbiddenException.class, this::authorize);
    }

    @Test void deniesSynchronousProviderFailure() {
        when(provider.authorize(context, "catalog.table.read", "sales/orders"))
                .thenThrow(new IllegalStateException("provider unavailable"));
        assertThrows(ForbiddenException.class, this::authorize);
    }
}
