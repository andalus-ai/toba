package com.dremioplatform.identity.repository;

import com.dremioplatform.identity.domain.PersonalAccessToken;
import com.dremioplatform.identity.domain.PrincipalType;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PersonalAccessTokenRepository implements PanacheRepositoryBase<PersonalAccessToken, UUID> {

    public Uni<PersonalAccessToken> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResult();
    }

    public Uni<List<PersonalAccessToken>> findByPrincipal(PrincipalType principalType, UUID principalId) {
        return find("principalType = ?1 and principalId = ?2", principalType, principalId).list();
    }
}
