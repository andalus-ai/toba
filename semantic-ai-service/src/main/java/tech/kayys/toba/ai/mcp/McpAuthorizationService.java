package com.dremioplatform.ai.mcp;

import com.dremioplatform.identity.grpc.AuthCheckService;
import com.dremioplatform.identity.grpc.CheckPermissionRequest;
import com.dremioplatform.identity.grpc.CheckPermissionsRequest;
import com.dremioplatform.identity.grpc.PathPermissionPair;
import com.dremioplatform.identity.grpc.PrincipalType;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * Per-tool authorization for MCP calls, reusing identity-access-service's real,
 * already-built {@code AuthCheckService} gRPC contract — the same one
 * {@code dremio-adapter}'s {@code AuthCheckGrpcClient} calls — rather than building
 * new authz logic in this service. Fails closed on any gRPC error (deny), mirroring
 * {@code AuthCheckGrpcClient}'s documented fail-closed behavior on the adapter side.
 *
 * The authenticated principal comes from the request's verified JWT (see
 * {@code quarkus-smallrye-jwt} config in application.properties — the same tokens
 * `identity-access-service`'s `TokenService` issues carry `sub` = principal id and
 * a custom `principalType` claim, which {@link #currentPrincipal} reads directly).
 */
@ApplicationScoped
public class McpAuthorizationService {

    private static final Logger LOG = Logger.getLogger(McpAuthorizationService.class);

    @GrpcClient("auth-check")
    AuthCheckService authCheckServiceClient;

    @Inject JsonWebToken jwt;

    public record Principal(PrincipalType type, String id) {
    }

    /** Reads the caller's identity from the verified JWT already established by
     *  the HTTP auth layer for this request — never trusts a client-supplied header. */
    public Principal currentPrincipal() {
        String rawType = jwt.getClaim("principalType");
        PrincipalType type = "SERVICE_ACCOUNT".equals(rawType) ? PrincipalType.SERVICE_ACCOUNT : PrincipalType.USER;
        return new Principal(type, jwt.getSubject());
    }

    public Uni<Boolean> checkPermission(String datasetPath, String requiredPermission) {
        Principal principal = currentPrincipal();
        CheckPermissionRequest request = CheckPermissionRequest.newBuilder()
                .setPrincipalType(principal.type())
                .setPrincipalId(principal.id())
                .setRequestedPath(datasetPath)
                .setRequiredPermission(requiredPermission)
                .build();
        return authCheckServiceClient.checkPermission(request)
                .map(response -> response.getAllowed())
                .onFailure().invoke(t -> LOG.warnf(t, "AuthCheckService.checkPermission failed for path=%s; failing closed (deny)", datasetPath))
                .onFailure().recoverWithItem(false);
    }

    /** Filters `datasetPaths` down to only those the current principal may access —
     *  used by search_datasets to post-filter semantic-search results rather than
     *  gating the search itself (a free-text query has no single scope path to
     *  check up front). */
    public Uni<List<String>> filterAuthorized(List<String> datasetPaths, String requiredPermission) {
        if (datasetPaths.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }
        Principal principal = currentPrincipal();
        List<PathPermissionPair> checks = datasetPaths.stream()
                .map(path -> PathPermissionPair.newBuilder()
                        .setRequestedPath(path)
                        .setRequiredPermission(requiredPermission)
                        .build())
                .toList();
        CheckPermissionsRequest request = CheckPermissionsRequest.newBuilder()
                .setPrincipalType(principal.type())
                .setPrincipalId(principal.id())
                .addAllChecks(checks)
                .build();

        return authCheckServiceClient.checkPermissions(request)
                .map(response -> {
                    List<String> allowed = new ArrayList<>();
                    List<Boolean> results = response.getResultsList();
                    for (int i = 0; i < datasetPaths.size(); i++) {
                        if (i < results.size() && Boolean.TRUE.equals(results.get(i))) {
                            allowed.add(datasetPaths.get(i));
                        }
                    }
                    return allowed;
                })
                .onFailure().invoke(t -> LOG.warnf(t, "AuthCheckService.checkPermissions failed; failing closed (deny all %d paths)", datasetPaths.size()))
                .onFailure().recoverWithItem(List.of());
    }
}
