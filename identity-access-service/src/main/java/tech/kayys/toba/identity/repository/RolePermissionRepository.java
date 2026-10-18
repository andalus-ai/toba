package com.dremioplatform.identity.repository;

import com.dremioplatform.identity.domain.RolePermission;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

/**
 * Deliberately exposes only flat, already-joined projections (never the raw entity's
 * lazy {@code Role.grantedPermissions} collection) — Hibernate Reactive sessions are
 * short-lived and stateless-adjacent, so touching a lazy collection outside the
 * original session throws. Every caller that needs "permissions for these roles"
 * goes through {@link #findPermissionKeysForRoles} instead.
 */
@ApplicationScoped
public class RolePermissionRepository implements PanacheRepositoryBase<RolePermission, UUID> {

    public Uni<List<String>> findPermissionKeysForRoles(List<UUID> roleIds) {
        if (roleIds.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }
        return getSession().chain(session -> session
                .createQuery(
                        "select distinct rp.permission.key from RolePermission rp where rp.role.id in ?1",
                        String.class)
                .setParameter(1, roleIds)
                .getResultList());
    }

    public Uni<List<String>> findPermissionKeysForRole(UUID roleId) {
        return findPermissionKeysForRoles(List.of(roleId));
    }
}
