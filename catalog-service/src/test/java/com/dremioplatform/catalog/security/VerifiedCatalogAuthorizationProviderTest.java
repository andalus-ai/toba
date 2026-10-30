package com.dremioplatform.catalog.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.SecurityContext;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerifiedCatalogAuthorizationProviderTest {
    private VerifiedCatalogAuthorizationProvider authorization;
    private CatalogIdentityVerifier identityVerifier;
    private CatalogMembershipGrantsVerifier grantsVerifier;
    private SecurityContext context;
    private VerifiedCatalogIdentity identity;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        authorization = new VerifiedCatalogAuthorizationProvider();
        identityVerifier = mock(CatalogIdentityVerifier.class);
        grantsVerifier = mock(CatalogMembershipGrantsVerifier.class);
        Instance<CatalogIdentityVerifier> identities = mock(Instance.class);
        Instance<CatalogMembershipGrantsVerifier> permissions = mock(Instance.class);
        when(identities.get()).thenReturn(identityVerifier);
        when(permissions.get()).thenReturn(grantsVerifier);
        authorization.identities = identities;
        authorization.permissions = permissions;
        context = mock(SecurityContext.class);
        identity = new VerifiedCatalogIdentity(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "subject", "issuer");
    }

    private UUID authorize() {
        return authorization.authorize(context, "catalog.table.read", "sales/orders")
                .await().indefinitely();
    }

    @Test void returnsCanonicalActorOnlyWhenBothStagesAllow() {
        when(identityVerifier.verify(context)).thenReturn(Uni.createFrom().item(identity));
        when(grantsVerifier.isAllowed(identity, "catalog.table.read", "sales/orders"))
                .thenReturn(Uni.createFrom().item(true));
        assertEquals(identity.userId(), authorize());
        verify(grantsVerifier).isAllowed(identity, "catalog.table.read", "sales/orders");
    }

    @Test void deniesExplicitPermissionRejection() {
        when(identityVerifier.verify(context)).thenReturn(Uni.createFrom().item(identity));
        when(grantsVerifier.isAllowed(identity, "catalog.table.read", "sales/orders"))
                .thenReturn(Uni.createFrom().item(false));
        assertThrows(ForbiddenException.class, this::authorize);
    }

    @Test void deniesMissingIdentity() {
        when(identityVerifier.verify(context)).thenReturn(Uni.createFrom().nullItem());
        assertThrows(ForbiddenException.class, this::authorize);
        verifyNoInteractions(grantsVerifier);
    }

    @Test void deniesMissingIdentityVerifier() {
        when(authorization.identities.isUnsatisfied()).thenReturn(true);
        assertThrows(ForbiddenException.class, this::authorize);
        verifyNoInteractions(identityVerifier, grantsVerifier);
    }

    @Test void deniesAmbiguousGrantVerifier() {
        when(authorization.permissions.isAmbiguous()).thenReturn(true);
        assertThrows(ForbiddenException.class, this::authorize);
        verifyNoInteractions(identityVerifier, grantsVerifier);
    }

    @Test void deniesIdentityVerifierFailure() {
        when(identityVerifier.verify(context)).thenReturn(
                Uni.createFrom().failure(new IllegalStateException("identity offline")));
        assertThrows(ForbiddenException.class, this::authorize);
        verifyNoInteractions(grantsVerifier);
    }

    @Test void deniesGrantVerifierFailure() {
        when(identityVerifier.verify(context)).thenReturn(Uni.createFrom().item(identity));
        when(grantsVerifier.isAllowed(identity, "catalog.table.read", "sales/orders"))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("policy offline")));
        assertThrows(ForbiddenException.class, this::authorize);
    }

    @Test void deniesNullGrantResult() {
        when(identityVerifier.verify(context)).thenReturn(Uni.createFrom().item(identity));
        when(grantsVerifier.isAllowed(identity, "catalog.table.read", "sales/orders"))
                .thenReturn(Uni.createFrom().nullItem());
        assertThrows(ForbiddenException.class, this::authorize);
    }

    @Test void deniesSynchronousIdentityFailure() {
        when(identityVerifier.verify(context)).thenThrow(new IllegalStateException("offline"));
        assertThrows(ForbiddenException.class, this::authorize);
    }

    @Test void deniesSynchronousGrantFailure() {
        when(identityVerifier.verify(context)).thenReturn(Uni.createFrom().item(identity));
        when(grantsVerifier.isAllowed(identity, "catalog.table.read", "sales/orders"))
                .thenThrow(new IllegalStateException("offline"));
        assertThrows(ForbiddenException.class, this::authorize);
    }

    @Test void deniesBlankActionBeforeCallingAdapters() {
        assertThrows(ForbiddenException.class, () -> authorization.authorize(context, " ",
                "sales/orders").await().indefinitely());
        verifyNoInteractions(identityVerifier, grantsVerifier);
    }
}
