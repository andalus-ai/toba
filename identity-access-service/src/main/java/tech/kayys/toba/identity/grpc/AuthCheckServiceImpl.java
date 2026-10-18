package com.dremioplatform.identity.grpc;

import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.service.PersonalAccessTokenService;
import com.dremioplatform.identity.service.RoleAssignmentService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implements the {@code AuthCheckService} contract in auth_check.proto. Base class
 * ({@code AuthCheckServiceGrpc.AuthCheckServiceImplBase} / Mutiny variant) is
 * generated at build time by quarkus-grpc from that .proto — not checked in here.
 *
 * Every method fails closed: any resolution error (unknown principal, malformed
 * credential, DB unavailable) results in {@code allowed = false} /
 * {@code valid = false} rather than propagating an exception that a caller might
 * mishandle as "allow". This mirrors the plan's stated risk mitigation for
 * policy-service unavailability (Section 12).
 */
@GrpcService
public class AuthCheckServiceImpl implements AuthCheckService {

    @Inject RoleAssignmentService roleAssignmentService;
    @Inject PersonalAccessTokenService personalAccessTokenService;
    @Inject com.dremioplatform.identity.service.TokenService tokenService;

    @Override
    public Uni<CheckPermissionResponse> checkPermission(CheckPermissionRequest request) {
        return roleAssignmentService.resolveEffectivePermissions(
                        toDomain(request.getPrincipalType()),
                        UUID.fromString(request.getPrincipalId()),
                        request.getRequestedPath())
                .map(perms -> CheckPermissionResponse.newBuilder()
                        .setAllowed(perms.contains(request.getRequiredPermission()))
                        .build())
                .onFailure().recoverWithItem(CheckPermissionResponse.newBuilder().setAllowed(false).build());
    }

    @Override
    public Uni<CheckPermissionsResponse> checkPermissions(CheckPermissionsRequest request) {
        PrincipalType principalType = toDomain(request.getPrincipalType());
        UUID principalId = UUID.fromString(request.getPrincipalId());

        // Resolve permissions per distinct requested_path once, then answer every
        // (path, permission) pair from that cached set — avoids N separate DB round
        // trips when a single query plan touches many tables under the same scope.
        List<PathPermissionPair> checks = request.getChecksList();
        List<String> distinctPaths = checks.stream().map(PathPermissionPair::getRequestedPath).distinct().toList();

        List<Uni<Set<String>>> lookups = distinctPaths.stream()
                .map(path -> roleAssignmentService.resolveEffectivePermissions(principalType, principalId, path))
                .toList();

        return Uni.join().all(lookups).andFailFast()
                .map(resultsPerPath -> {
                    var byPath = new java.util.HashMap<String, Set<String>>();
                    for (int i = 0; i < distinctPaths.size(); i++) {
                        byPath.put(distinctPaths.get(i), resultsPerPath.get(i));
                    }
                    CheckPermissionsResponse.Builder builder = CheckPermissionsResponse.newBuilder();
                    for (PathPermissionPair pair : checks) {
                        Set<String> perms = byPath.getOrDefault(pair.getRequestedPath(), Set.of());
                        builder.addResults(perms.contains(pair.getRequiredPermission()));
                    }
                    return builder.build();
                })
                .onFailure().recoverWithItem(() -> {
                    CheckPermissionsResponse.Builder builder = CheckPermissionsResponse.newBuilder();
                    checks.forEach(c -> builder.addResults(false));
                    return builder.build();
                });
    }

    @Override
    public Uni<ResolvePrincipalResponse> resolvePrincipal(ResolvePrincipalRequest request) {
        String credential = request.getCredential();
        if (credential.startsWith("pat_")) {
            return personalAccessTokenService.validateAndTouch(credential)
                    .map(pat -> ResolvePrincipalResponse.newBuilder()
                            .setValid(true)
                            .setPrincipalType(fromDomain(pat.principalType()))
                            .setPrincipalId(pat.principalId().toString())
                            .setDisplayName(pat.label())
                            .build())
                    .onFailure().recoverWithItem(ResolvePrincipalResponse.newBuilder().setValid(false).build());
        }
        // JWT path: verified via the same signing key TokenService issued it with.
        return tokenService.verify(credential)
                .map(principal -> ResolvePrincipalResponse.newBuilder()
                        .setValid(true)
                        .setPrincipalType(fromDomain(principal.principalType()))
                        .setPrincipalId(principal.principalId().toString())
                        .setDisplayName(principal.displayName() == null ? "" : principal.displayName())
                        .build())
                .onFailure().recoverWithItem(ResolvePrincipalResponse.newBuilder().setValid(false).build());
    }

    private static PrincipalType toDomain(com.dremioplatform.identity.grpc.PrincipalType grpcType) {
        return grpcType == com.dremioplatform.identity.grpc.PrincipalType.SERVICE_ACCOUNT
                ? PrincipalType.SERVICE_ACCOUNT
                : PrincipalType.USER;
    }

    private static com.dremioplatform.identity.grpc.PrincipalType fromDomain(PrincipalType domainType) {
        return domainType == PrincipalType.SERVICE_ACCOUNT
                ? com.dremioplatform.identity.grpc.PrincipalType.SERVICE_ACCOUNT
                : com.dremioplatform.identity.grpc.PrincipalType.USER;
    }
}
