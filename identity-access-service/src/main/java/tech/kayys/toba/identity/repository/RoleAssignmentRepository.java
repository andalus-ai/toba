package com.dremioplatform.identity.repository;

import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.domain.RoleAssignment;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RoleAssignmentRepository implements PanacheRepositoryBase<RoleAssignment, UUID> {

    /**
     * Every non-expired role granted to this principal at {@code requestedPath} itself
     * OR at any ancestor scope (e.g. an assignment scoped to "space.sales" applies to
     * "space.sales.folder.q4.dataset.orders" too). Grants are additive/cascading down
     * the hierarchy — there is no explicit-deny concept in this model; the Governance
     * Service's masking policies are the mechanism for narrowing what a broad grant
     * can see, not this table.
     *
     * The "." + requestedPath LIKE scopePath + '.%' check enforces a segment boundary
     * so "space.salesforce" does not match a grant scoped to "space.sales".
     */
    public Uni<List<UUID>> findApplicableRoleIds(PrincipalType principalType, UUID principalId, String requestedPath) {
        return getSession().chain(session -> session
                .createQuery(
                        """
                        select distinct ra.role.id
                        from RoleAssignment ra
                        where ra.principalType = ?1
                          and ra.principalId = ?2
                          and (ra.expiresAt is null or ra.expiresAt > ?3)
                          and (ra.scopePath = ?4 or ?4 like concat(ra.scopePath, '.%'))
                        """,
                        UUID.class)
                .setParameter(1, principalType)
                .setParameter(2, principalId)
                .setParameter(3, Instant.now())
                .setParameter(4, requestedPath)
                .getResultList());
    }

    public Uni<List<RoleAssignment>> findByPrincipal(PrincipalType principalType, UUID principalId) {
        return find("principalType = ?1 and principalId = ?2", principalType, principalId).list();
    }

    public Uni<Long> deleteByPrincipalAndRole(PrincipalType principalType, UUID principalId, UUID roleId) {
        return delete("principalType = ?1 and principalId = ?2 and role.id = ?3", principalType, principalId, roleId);
    }
}
