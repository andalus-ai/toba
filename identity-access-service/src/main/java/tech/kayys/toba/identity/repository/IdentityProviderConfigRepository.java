package com.dremioplatform.identity.repository;

import com.dremioplatform.identity.domain.IdentityProviderConfig;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class IdentityProviderConfigRepository implements PanacheRepositoryBase<IdentityProviderConfig, UUID> {

    public Uni<List<IdentityProviderConfig>> findEnabled() {
        return find("enabled", true).list();
    }

    public Uni<IdentityProviderConfig> findByName(String name) {
        return find("name", name).firstResult();
    }
}
