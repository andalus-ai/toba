package com.dremioplatform.ai.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dremioplatform.identity.grpc.AuthCheckService;
import com.dremioplatform.identity.grpc.CheckPermissionResponse;
import com.dremioplatform.identity.grpc.CheckPermissionsResponse;
import com.dremioplatform.identity.grpc.PrincipalType;
import io.smallrye.mutiny.Uni;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

/**
 * Plain unit test (no @QuarkusTest, no CDI container) — mocks the gRPC client and
 * JWT directly, since McpAuthorizationService's fields are package-visible. Focuses
 * on the two behaviors that matter most: fail-closed on gRPC error, and correct
 * index alignment in filterAuthorized's bulk-result mapping.
 */
class McpAuthorizationServiceTest {

    @Test
    void checkPermission_grpcSucceeds_returnsAllowedValue() {
        AuthCheckService client = mock(AuthCheckService.class);
        when(client.checkPermission(any())).thenReturn(
                Uni.createFrom().item(CheckPermissionResponse.newBuilder().setAllowed(true).build()));

        McpAuthorizationService service = newService(client, "user-1", "USER");

        Boolean result = service.checkPermission("space.sales.orders", "catalog.dataset.read")
                .await().indefinitely();

        assertTrue(result);
    }

    @Test
    void checkPermission_grpcFails_failsClosedToFalse() {
        AuthCheckService client = mock(AuthCheckService.class);
        when(client.checkPermission(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("network error")));

        McpAuthorizationService service = newService(client, "user-1", "USER");

        Boolean result = service.checkPermission("space.sales.orders", "catalog.dataset.read")
                .await().indefinitely();

        assertFalse(result, "A gRPC failure must fail closed (deny), never silently allow");
    }

    @Test
    void filterAuthorized_returnsOnlyPathsMarkedTrue_inOriginalOrder() {
        AuthCheckService client = mock(AuthCheckService.class);
        when(client.checkPermissions(any())).thenReturn(Uni.createFrom().item(
                CheckPermissionsResponse.newBuilder()
                        .addResults(true)   // space.a.t1
                        .addResults(false)  // space.a.t2
                        .addResults(true)   // space.a.t3
                        .build()));

        McpAuthorizationService service = newService(client, "user-1", "USER");

        List<String> allowed = service.filterAuthorized(
                        List.of("space.a.t1", "space.a.t2", "space.a.t3"), "catalog.dataset.read")
                .await().indefinitely();

        assertEquals(List.of("space.a.t1", "space.a.t3"), allowed);
    }

    @Test
    void filterAuthorized_grpcFails_failsClosedToEmptyList() {
        AuthCheckService client = mock(AuthCheckService.class);
        when(client.checkPermissions(any())).thenReturn(Uni.createFrom().failure(new RuntimeException("timeout")));

        McpAuthorizationService service = newService(client, "user-1", "USER");

        List<String> allowed = service.filterAuthorized(List.of("space.a.t1", "space.a.t2"), "catalog.dataset.read")
                .await().indefinitely();

        assertTrue(allowed.isEmpty(), "A gRPC failure must fail closed (deny all), never silently allow any");
    }

    @Test
    void currentPrincipal_readsServiceAccountClaimCorrectly() {
        McpAuthorizationService service = newService(mock(AuthCheckService.class), "svc-42", "SERVICE_ACCOUNT");

        McpAuthorizationService.Principal principal = service.currentPrincipal();

        assertEquals(PrincipalType.SERVICE_ACCOUNT, principal.type());
        assertEquals("svc-42", principal.id());
    }

    private static McpAuthorizationService newService(AuthCheckService client, String subject, String principalType) {
        McpAuthorizationService service = new McpAuthorizationService();
        service.authCheckServiceClient = client;

        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwt.getClaim("principalType")).thenReturn(principalType);
        service.jwt = jwt;

        return service;
    }
}
