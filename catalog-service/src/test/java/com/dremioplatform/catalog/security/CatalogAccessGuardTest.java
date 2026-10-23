package com.dremioplatform.catalog.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import org.junit.jupiter.api.Test;

class CatalogAccessGuardTest {
    private final CatalogAccessGuard guard = new CatalogAccessGuard();

    @Test void deniesMissingSecurityContext() {
        assertThrows(ForbiddenException.class, () -> guard.require(null, "catalog.read", "tables"));
    }

    @Test void deniesUnauthenticatedContext() {
        SecurityContext context = mock(SecurityContext.class);
        assertThrows(ForbiddenException.class, () -> guard.require(context, "catalog.read", "tables"));
    }

    @Test void deniesAuthenticatedPrincipalWithoutAuthorizationEvidence() {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getUserPrincipal()).thenReturn((Principal) () -> "authenticated-user");
        assertThrows(ForbiddenException.class, () -> guard.require(context, "catalog.read", "tables"));
    }

    @Test void deniesBlankAction() {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getUserPrincipal()).thenReturn((Principal) () -> "authenticated-user");
        assertThrows(ForbiddenException.class, () -> guard.require(context, " ", "tables"));
    }
}
