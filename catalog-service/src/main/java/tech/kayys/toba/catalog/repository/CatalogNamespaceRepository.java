package com.dremioplatform.catalog.repository;

import com.dremioplatform.catalog.domain.CatalogNamespace;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CatalogNamespaceRepository implements PanacheRepositoryBase<CatalogNamespace, UUID> {

    public Uni<CatalogNamespace> findByLevelsPath(String levelsPath) {
        return find("levelsPath", levelsPath).firstResult();
    }

    public Uni<Boolean> existsByLevelsPath(String levelsPath) {
        return count("levelsPath", levelsPath).map(c -> c > 0);
    }

    /** Direct children only (one level deeper), matching the Iceberg REST spec's
     *  "list namespaces under parent" semantics (not a full subtree). */
    public Uni<List<CatalogNamespace>> findChildrenOf(String parentLevelsPath) {
        if (parentLevelsPath == null || parentLevelsPath.isBlank()) {
            // Top-level: any namespace with no dot in its path.
            return find("levelsPath not like '%.%'").list();
        }
        String likePattern = parentLevelsPath + ".%";
        return find("levelsPath like ?1 and levelsPath not like ?2", likePattern, parentLevelsPath + ".%.%").list();
    }
}
