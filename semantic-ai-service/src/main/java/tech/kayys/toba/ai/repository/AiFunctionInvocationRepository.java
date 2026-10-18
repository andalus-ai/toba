package com.dremioplatform.ai.repository;

import com.dremioplatform.ai.domain.AiFunctionInvocation;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AiFunctionInvocationRepository implements PanacheRepositoryBase<AiFunctionInvocation, UUID> {

    public Uni<List<AiFunctionInvocation>> findByPrincipal(String principalType, String principalId,
                                                              Instant since, Instant until, int limit) {
        return find("principalType = ?1 and principalId = ?2 and invokedAt >= ?3 and invokedAt <= ?4 order by invokedAt desc",
                principalType, principalId, since, until).page(0, limit).list();
    }

    /** Usage/cost-tracking rollup — total prompt+completion tokens for a principal
     *  in a time range. Used by the (not yet built) budget-alert piece — see module
     *  README "Known gaps". */
    public Uni<Long> sumTokensForPrincipal(String principalType, String principalId, Instant since, Instant until) {
        return getSession().chain(session -> session
                .createQuery(
                        "select coalesce(sum(coalesce(a.promptTokens,0) + coalesce(a.completionTokens,0)), 0) "
                                + "from AiFunctionInvocation a "
                                + "where a.principalType = ?1 and a.principalId = ?2 and a.invokedAt >= ?3 and a.invokedAt <= ?4",
                        Long.class)
                .setParameter(1, principalType)
                .setParameter(2, principalId)
                .setParameter(3, since)
                .setParameter(4, until)
                .getSingleResult());
    }
}
