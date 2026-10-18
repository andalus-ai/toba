package com.dremioplatform.adapter.grpc;

import com.dremioplatform.adapter.config.AdapterConfig;
import com.dremioplatform.identity.grpc.AuthCheckServiceGrpc;
import com.dremioplatform.identity.grpc.CheckPermissionRequest;
import com.dremioplatform.identity.grpc.CheckPermissionResponse;
import com.dremioplatform.identity.grpc.CheckPermissionsRequest;
import com.dremioplatform.identity.grpc.CheckPermissionsResponse;
import com.dremioplatform.identity.grpc.PathPermissionPair;
import com.dremioplatform.identity.grpc.PrincipalType;
import com.dremioplatform.identity.grpc.ResolvePrincipalRequest;
import com.dremioplatform.identity.grpc.ResolvePrincipalResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The synchronous, inline call every SPI method in {@link com.dremioplatform.adapter.spi}
 * makes into identity-access-service. Uses the blocking stub deliberately — Dremio's
 * SPI methods (AccessControlChecker, etc.) are themselves synchronous, so there is
 * no benefit to an async stub here and it would only add complexity translating
 * between Dremio's synchronous call convention and a Future/reactive one.
 *
 * FAILS CLOSED: every method catches all gRPC/timeout failures and returns the
 * "deny" / "invalid" result rather than propagating — see the plan's Section 12
 * risk mitigation ("fail-closed default (deny/mask on policy-service unavailability)").
 * This mirrors AuthCheckServiceImpl's own fail-closed behavior server-side; the
 * client-side copy here is the second layer of that same guarantee (network
 * partition between Dremio and identity-access-service must not silently become
 * "allow everything").
 */
public class AuthCheckGrpcClient implements Closeable {

    private static final Logger LOG = Logger.getLogger(AuthCheckGrpcClient.class.getName());

    private final ManagedChannel channel;
    private final AuthCheckServiceGrpc.AuthCheckServiceBlockingStub stub;
    private final long timeoutMillis;

    public AuthCheckGrpcClient(AdapterConfig config) {
        this.channel = ManagedChannelBuilder
                .forAddress(config.identityGrpcHost(), config.identityGrpcPort())
                // Real deployments terminate TLS here (mutual TLS between services —
                // plan Section 9 "Secrets/keys"); plaintext is for local dev only.
                .usePlaintext()
                .build();
        this.stub = AuthCheckServiceGrpc.newBlockingStub(channel);
        this.timeoutMillis = config.authCheckTimeoutMillis();
    }

    public boolean checkPermission(PrincipalType principalType, String principalId, String requestedPath, String requiredPermission) {
        try {
            CheckPermissionRequest request = CheckPermissionRequest.newBuilder()
                    .setPrincipalType(principalType)
                    .setPrincipalId(principalId)
                    .setRequestedPath(requestedPath)
                    .setRequiredPermission(requiredPermission)
                    .build();
            CheckPermissionResponse response = stub
                    .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                    .checkPermission(request);
            return response.getAllowed();
        } catch (StatusRuntimeException | RuntimeException e) {
            LOG.log(Level.WARNING, "AuthCheckService.checkPermission failed; failing closed (deny)", e);
            return false;
        }
    }

    /** Batched variant — see AuthCheckServiceImpl's Javadoc for why this exists
     *  (avoids one round trip per referenced table in a query plan). */
    public List<Boolean> checkPermissions(PrincipalType principalType, String principalId,
                                            List<PathPermissionPair> checks) {
        try {
            CheckPermissionsRequest request = CheckPermissionsRequest.newBuilder()
                    .setPrincipalType(principalType)
                    .setPrincipalId(principalId)
                    .addAllChecks(checks)
                    .build();
            CheckPermissionsResponse response = stub
                    .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                    .checkPermissions(request);
            return response.getResultsList();
        } catch (StatusRuntimeException | RuntimeException e) {
            LOG.log(Level.WARNING, "AuthCheckService.checkPermissions failed; failing closed (deny all)", e);
            return checks.stream().map(c -> Boolean.FALSE).toList();
        }
    }

    public ResolvedPrincipal resolvePrincipal(String credential) {
        try {
            ResolvePrincipalResponse response = stub
                    .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
                    .resolvePrincipal(ResolvePrincipalRequest.newBuilder().setCredential(credential).build());
            if (!response.getValid()) {
                return ResolvedPrincipal.invalid();
            }
            return new ResolvedPrincipal(true, response.getPrincipalType(), response.getPrincipalId(), response.getDisplayName());
        } catch (StatusRuntimeException | RuntimeException e) {
            LOG.log(Level.WARNING, "AuthCheckService.resolvePrincipal failed; failing closed (invalid)", e);
            return ResolvedPrincipal.invalid();
        }
    }

    @Override
    public void close() {
        channel.shutdown();
        try {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public record ResolvedPrincipal(boolean valid, PrincipalType principalType, String principalId, String displayName) {
        public static ResolvedPrincipal invalid() {
            return new ResolvedPrincipal(false, PrincipalType.PRINCIPAL_TYPE_UNSPECIFIED, null, null);
        }
    }
}
