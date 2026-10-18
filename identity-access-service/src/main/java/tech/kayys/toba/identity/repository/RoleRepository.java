package com.dremioplatform.identity.repository;

import com.dremioplatform.identity.domain.Role;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class RoleRepository implements PanacheRepositoryBase<Role, UUID> {

    public Uni<Role> findByName(String name) {
        return find("name", name).firstResult();
    }

    public Uni<Boolean> existsByName(String name) {
        return count("name", name).map(c -> c > 0);
    }
}
