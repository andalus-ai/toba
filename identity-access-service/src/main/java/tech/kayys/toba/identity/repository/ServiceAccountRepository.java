package com.dremioplatform.identity.repository;

import com.dremioplatform.identity.domain.ServiceAccount;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class ServiceAccountRepository implements PanacheRepositoryBase<ServiceAccount, UUID> {

    public Uni<ServiceAccount> findByName(String name) {
        return find("name", name).firstResult();
    }

    public Uni<Boolean> existsByName(String name) {
        return count("name", name).map(c -> c > 0);
    }
}
