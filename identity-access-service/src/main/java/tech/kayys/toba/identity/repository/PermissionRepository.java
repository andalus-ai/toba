package com.dremioplatform.identity.repository;

import com.dremioplatform.identity.domain.Permission;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PermissionRepository implements PanacheRepositoryBase<Permission, UUID> {

    public Uni<Permission> findByKey(String key) {
        return find("key", key).firstResult();
    }

    public Uni<List<Permission>> findByDomain(String domain) {
        return find("domain", domain).list();
    }

    public Uni<List<Permission>> findByKeys(List<String> keys) {
        return find("key in ?1", keys).list();
    }
}
