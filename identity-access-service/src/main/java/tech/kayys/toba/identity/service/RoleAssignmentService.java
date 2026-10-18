package com.dremioplatform.identity.service;

import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.domain.Role;
import com.dremioplatform.identity.domain.RoleAssignment;
import com.dremioplatform.identity.event.EventPublisher;
import com.dremioplatform.identity.event.RoleAssignmentChangedEvent;
import com.dremioplatform.identity.repository.RoleAssignmentRepository;
import com.dremioplatform.identity.repository.RolePermissionRepository;
import com.dremioplatform.identity.repository.RoleRepository;
import com.dremioplatform.identity.service.dto.GrantRoleAssignmentRequest;
import com.dremioplatform.identity.service.dto.RoleAssignmentResponse;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RoleAssignmentService {

    @Inject RoleAssignmentRepository roleAssignmentRepository;
    @Inject RoleRepository roleRepository;
    @Inject RolePermissionRepository rolePermissionRepository;
    @Inject EventPublisher eventPublisher;

    @WithTransaction
    public Uni<RoleAssignmentResponse> grant(GrantRoleAssignmentRequest request, UUID actorUserId) {
        return roleRepository.findById(request.roleId())
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "IDENTITY-404-ROLE-NOT-FOUND", "Role", request.roleId()))
                .chain(role -> {
                    RoleAssignment assignment = new RoleAssignment();
                    assignment.id = UUID.randomUUID();
                    assignment.principalType = request.principalType();
                    assignment.principalId = request.principalId();
                    assignment.role = role;
                    assignment.scopeType = request.scopeType();
                    assignment.scopePath = request.scopePath();
                    assignment.grantedByUserId = actorUserId;
                    assignment.expiresAt = request.expiresAt();
                    return roleAssignmentRepository.persist(assignment)
                            .call(a -> eventPublisher.publish(RoleAssignmentChangedEvent.granted(
                                    a.principalType, a.principalId, role.id, a.scopePath, actorUserId)))
                            .map(RoleAssignmentResponse::from);
                });
    }

    @WithTransaction
    public Uni<Void> revoke(PrincipalType principalType, UUID principalId, UUID roleId, UUID actorUserId, String scopePath) {
        return roleAssignmentRepository.deleteByPrincipalAndRole(principalType, principalId, roleId)
                .chain(deletedCount -> {
                    if (deletedCount == 0) {
                        return Uni.createFrom().failure(new EntityNotFoundException(
                                "IDENTITY-404-ASSIGNMENT-NOT-FOUND", "RoleAssignment", roleId));
                    }
                    return eventPublisher.publish(RoleAssignmentChangedEvent.revoked(
                            principalType, principalId, roleId, scopePath, actorUserId));
                });
    }

    public Uni<List<RoleAssignmentResponse>> listForPrincipal(PrincipalType principalType, UUID principalId) {
        return roleAssignmentRepository.findByPrincipal(principalType, principalId)
                .map(list -> list.stream().map(RoleAssignmentResponse::from).toList());
    }

    /**
     * The resolution the gRPC AuthCheckService calls on every Dremio coordinator
     * request: union the permission keys granted by every role applicable to this
     * principal at this scope-path-or-ancestor (see RoleAssignmentRepository's query
     * doc comment for the cascading-down semantics).
     */
    public Uni<Set<String>> resolveEffectivePermissions(PrincipalType principalType, UUID principalId, String requestedPath) {
        return roleAssignmentRepository.findApplicableRoleIds(principalType, principalId, requestedPath)
                .chain(rolePermissionRepository::findPermissionKeysForRoles)
                .map(Set::copyOf);
    }
}
