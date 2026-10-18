package com.dremioplatform.catalog.repository;

import com.dremioplatform.catalog.domain.CatalogTable;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CatalogTableRepository implements PanacheRepositoryBase<CatalogTable, UUID> {

    public Uni<CatalogTable> findByNamespaceAndName(String namespacePath, String tableName) {
        return find("namespacePath = ?1 and tableName = ?2", namespacePath, tableName).firstResult();
    }

    public Uni<List<CatalogTable>> listInNamespace(String namespacePath) {
        return find("namespacePath", namespacePath).list();
    }

    public Uni<Boolean> exists(String namespacePath, String tableName) {
        return count("namespacePath = ?1 and tableName = ?2", namespacePath, tableName).map(c -> c > 0);
    }
}
