package com.dremioplatform.governance.repository;

import com.dremioplatform.governance.domain.MaskingPolicy;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MaskingPolicyRepository implements PanacheRepositoryBase<MaskingPolicy, UUID> {

    /**
     * Every enabled policy whose scopePath is an ancestor-or-equal of the queried
     * path — same cascading-scope semantics as Identity's RoleAssignment, so a
     * masking policy defined at "space.sales" applies to every dataset under it.
     */
    public Uni<List<MaskingPolicy>> findApplicable(String requestedPath) {
        return find("enabled = true and (scopePath = ?1 or ?1 like concat(scopePath, '.%'))", requestedPath).list();
    }
}
