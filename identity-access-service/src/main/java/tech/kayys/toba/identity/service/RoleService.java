package com.dremioplatform.identity.service;

import com.dremioplatform.common.error.ConflictException;
import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.common.error.ValidationException;
import com.dremioplatform.identity.domain.Permission;
import com.dremioplatform.identity.domain.Role;
import com.dremioplatform.identity.domain.RolePermission;
import com.dremioplatform.identity.repository.PermissionRepository;
import com.dremioplatform.identity.repository.RolePermissionRepository;
import com.dremioplatform.identity.repository.RoleRepository;
import com.dremioplatform.identity.service.dto.CreateRoleRequest;
import com.dremioplatform.identity.service.dto.RoleResponse;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RoleService {

    @Inject RoleRepository roleRepository;
    @Inject PermissionRepository permissionRepository;
    @Inject RolePermissionRepository rolePermissionRepository;

    @WithTransaction
    public Uni<RoleResponse> createRole(CreateRoleRequest request) {
        return roleRepository.existsByName(request.name())
                .chain(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(new ConflictException(
                                "IDENTITY-409-ROLE-EXISTS", "A role named '" + request.name() + "' already exists"));
                    }
                    return permissionRepository.findByKeys(request.permissionKeys())
                            .chain(found -> {
                                if (found.size() != request.permissionKeys().size()) {
                                    return Uni.createFrom().failure(new ValidationException(
                                            "IDENTITY-400-UNKNOWN-PERMISSION",
                                            "One or more permissionKeys are not registered in the permission catalog"));
                                }
                                Role role = new Role();
                                role.id = UUID.randomUUID();
                                role.name = request.name();
                                role.description = request.description();
                                return roleRepository.persist(role).chain(persisted -> attachPermissions(persisted, found));
                            });
                })
                .map(pair -> RoleResponse.from(pair.role, pair.permissionKeys));
    }

    private Uni<RoleWithKeys> attachPermissions(Role role, List<Permission> permissions) {
        List<Uni<RolePermission>> inserts = permissions.stream().map(p -> {
            RolePermission rp = new RolePermission();
            rp.id = UUID.randomUUID();
            rp.role = role;
            rp.permission = p;
            return rolePermissionRepository.persist(rp);
        }).toList();
        return Uni.join().all(inserts).andFailFast()
                .map(ignored -> new RoleWithKeys(role, permissions.stream().map(p -> p.key).toList()));
    }

    public Uni<RoleResponse> getById(UUID roleId) {
        return roleRepository.findById(roleId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException("IDENTITY-404-ROLE-NOT-FOUND", "Role", roleId))
                .chain(role -> rolePermissionRepository.findPermissionKeysForRole(role.id)
                        .map(keys -> RoleResponse.from(role, keys)));
    }

    public Uni<List<RoleResponse>> listAll() {
        return roleRepository.listAll()
                .chain(roles -> Uni.join().all(
                        roles.stream()
                                .map(r -> rolePermissionRepository.findPermissionKeysForRole(r.id)
                                        .map(keys -> RoleResponse.from(r, keys)))
                                .toList()
                ).andFailFast());
    }

    @WithTransaction
    public Uni<Void> deleteRole(UUID roleId) {
        return roleRepository.findById(roleId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException("IDENTITY-404-ROLE-NOT-FOUND", "Role", roleId))
                .chain(role -> {
                    if (role.systemRole) {
                        return Uni.createFrom().failure(new ConflictException(
                                "IDENTITY-409-SYSTEM-ROLE-IMMUTABLE", "System roles cannot be deleted"));
                    }
                    return roleRepository.delete(role);
                });
    }

    private record RoleWithKeys(Role role, List<String> permissionKeys) {
    }
}
