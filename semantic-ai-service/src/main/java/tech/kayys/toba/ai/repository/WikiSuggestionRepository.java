package com.dremioplatform.ai.repository;

import com.dremioplatform.ai.domain.WikiSuggestion;
import com.dremioplatform.ai.domain.WikiSuggestionStatus;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class WikiSuggestionRepository implements PanacheRepositoryBase<WikiSuggestion, UUID> {

    public Uni<List<WikiSuggestion>> findByStatus(WikiSuggestionStatus status, int limit) {
        return find("status = ?1 order by createdAt asc", status).page(0, limit).list();
    }

    public Uni<List<WikiSuggestion>> findByDatasetPath(String datasetPath) {
        return find("datasetPath = ?1 order by createdAt desc", datasetPath).list();
    }
}
