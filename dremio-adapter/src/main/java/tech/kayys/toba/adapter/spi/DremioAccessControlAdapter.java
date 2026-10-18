package com.dremioplatform.adapter.spi;

import com.dremioplatform.adapter.grpc.AuthCheckGrpcClient;
import com.dremioplatform.identity.grpc.PathPermissionPair;
import com.dremioplatform.identity.grpc.PrincipalType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registered with Dremio (see module README "Wiring this into a real Dremio OSS
 * build") in place of / alongside OSS's default coarse read/write ACL checker.
 * Every method is a single inline call to {@link AuthCheckGrpcClient}, which itself
 * fails closed on any network/timeout error — this class adds no additional
 * fail-open/closed logic of its own, it just translates between Dremio's principal/
 * path/operation vocabulary and the gRPC contract's.
 *
 * `operation` strings (e.g. "READ", "WRITE") are mapped to Identity's permission
 * keys (e.g. "catalog.dataset.read") via {@link #toPermissionKey} — kept as an
 * explicit, small mapping table rather than a naming convention, since Dremio's
 * real operation vocabulary (once reconciled against the actual SPI) may not line
 * up 1:1 with Identity's permission-key domain.
 */
public class DremioAccessControlAdapter implements DremioAccessControlChecker {

    private final AuthCheckGrpcClient authCheckGrpcClient;

    public DremioAccessControlAdapter(AuthCheckGrpcClient authCheckGrpcClient) {
        this.authCheckGrpcClient = authCheckGrpcClient;
    }

    @Override
    public boolean hasAccess(String principalType, String principalId, String datasetPath, String operation) {
        return authCheckGrpcClient.checkPermission(
                toGrpcPrincipalType(principalType), principalId, datasetPath, toPermissionKey(operation));
    }

    @Override
    public Map<String, Boolean> hasAccessBulk(String principalType, String principalId,
                                                List<String> datasetPaths, String operation) {
        String permissionKey = toPermissionKey(operation);
        List<PathPermissionPair> checks = datasetPaths.stream()
                .map(path -> PathPermissionPair.newBuilder()
                        .setRequestedPath(path)
                        .setRequiredPermission(permissionKey)
                        .build())
                .toList();

        List<Boolean> results = authCheckGrpcClient.checkPermissions(
                toGrpcPrincipalType(principalType), principalId, checks);

        Map<String, Boolean> byPath = new HashMap<>();
        for (int i = 0; i < datasetPaths.size(); i++) {
            byPath.put(datasetPaths.get(i), results.get(i));
        }
        return byPath;
    }

    private static PrincipalType toGrpcPrincipalType(String principalType) {
        return "SERVICE_ACCOUNT".equalsIgnoreCase(principalType) ? PrincipalType.SERVICE_ACCOUNT : PrincipalType.USER;
    }

    /** See class Javadoc — this mapping is deliberately explicit and small; extend
     *  it here (not by convention) as more Dremio operation kinds need mapping. */
    private static String toPermissionKey(String operation) {
        return switch (operation.toUpperCase()) {
            case "READ", "SELECT" -> "catalog.dataset.read";
            case "WRITE", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP" -> "catalog.dataset.write";
            case "MANAGE_NAMESPACE" -> "catalog.namespace.manage";
            default -> throw new IllegalArgumentException("Unmapped Dremio operation: " + operation);
        };
    }
}
