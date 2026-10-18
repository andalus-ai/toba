package com.dremioplatform.adapter.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dremioplatform.adapter.grpc.AuthCheckGrpcClient;
import com.dremioplatform.identity.grpc.PrincipalType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DremioAccessControlAdapterTest {

    @Test
    void hasAccess_mapsReadOperationToCatalogReadPermission() {
        AuthCheckGrpcClient client = mock(AuthCheckGrpcClient.class);
        when(client.checkPermission(PrincipalType.USER, "user-1", "space.sales.orders", "catalog.dataset.read"))
                .thenReturn(true);

        DremioAccessControlAdapter adapter = new DremioAccessControlAdapter(client);

        assertTrue(adapter.hasAccess("USER", "user-1", "space.sales.orders", "READ"));
    }

    @Test
    void hasAccess_grpcClientDeniesOnFailure_propagatesDenial() {
        AuthCheckGrpcClient client = mock(AuthCheckGrpcClient.class);
        when(client.checkPermission(PrincipalType.USER, "user-1", "space.sales.orders", "catalog.dataset.write"))
                .thenReturn(false); // simulates the client's own fail-closed behavior on a network error

        DremioAccessControlAdapter adapter = new DremioAccessControlAdapter(client);

        assertFalse(adapter.hasAccess("USER", "user-1", "space.sales.orders", "WRITE"));
    }

    @Test
    void hasAccess_unmappedOperation_throws() {
        AuthCheckGrpcClient client = mock(AuthCheckGrpcClient.class);
        DremioAccessControlAdapter adapter = new DremioAccessControlAdapter(client);

        assertThrows(IllegalArgumentException.class, () ->
                adapter.hasAccess("USER", "user-1", "space.sales.orders", "TELEPORT"));
    }

    @Test
    void hasAccessBulk_returnsResultsAlignedToRequestedPaths() {
        AuthCheckGrpcClient client = mock(AuthCheckGrpcClient.class);
        when(client.checkPermissions(org.mockito.ArgumentMatchers.eq(PrincipalType.USER),
                org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(true, false));

        DremioAccessControlAdapter adapter = new DremioAccessControlAdapter(client);
        Map<String, Boolean> result = adapter.hasAccessBulk(
                "USER", "user-1", List.of("space.a.t1", "space.a.t2"), "READ");

        assertEquals(Map.of("space.a.t1", true, "space.a.t2", false), result);
    }
}
