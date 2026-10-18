package com.dremioplatform.identity.repository;

import com.dremioplatform.identity.domain.User;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    public Uni<User> findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public Uni<User> findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public Uni<User> findByExternalIdentity(UUID identityProviderId, String externalSubject) {
        return find("identityProviderId = ?1 and externalSubject = ?2", identityProviderId, externalSubject)
                .firstResult();
    }

    public Uni<Boolean> existsByUsernameOrEmail(String username, String email) {
        return count("username = ?1 or email = ?2", username, email).map(c -> c > 0);
    }
}
