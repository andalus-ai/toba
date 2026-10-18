package com.dremioplatform.ai.event;

import com.dremioplatform.ai.domain.EmbeddingTargetType;
import com.dremioplatform.ai.service.EmbeddingService;
import com.dremioplatform.ai.service.dto.UpsertEmbeddingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes catalog-service's `catalog.events` topic (see that service's
 * `CatalogEventPublisher`) and generates a starter embedding for every newly
 * created table — plan Section 5.5: "Embedding pipeline: on catalog/wiki/schema
 * change events, generate embeddings ... for semantic search over datasets/columns/wikis."
 *
 * The embedded `sourceText` here is deliberately minimal (just the table's
 * namespace/name path) — a real pipeline would fetch the table's actual schema
 * (column names/types) from catalog-service's `LoadTableResponse` and embed a
 * richer description. That enrichment is flagged in module README "Known gaps"
 * rather than implemented here, since it needs an HTTP client call to
 * catalog-service this increment doesn't add. `TABLE_DROPPED` events are logged
 * but don't delete the embedding — stale embeddings for dropped tables are a
 * correctness gap noted in the same README section.
 */
@ApplicationScoped
public class CatalogEventConsumer {

    private static final Logger LOG = Logger.getLogger(CatalogEventConsumer.class);

    @Inject EmbeddingService embeddingService;
    @Inject ObjectMapper objectMapper;

    @Incoming("catalog-events-in")
    public Uni<Void> onCatalogEvent(String rawJson) {
        JsonNode node;
        try {
            node = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            LOG.errorf(e, "Skipping unparseable catalog event: %s", rawJson);
            return Uni.createFrom().voidItem();
        }

        String eventType = node.path("eventType").asText("");
        if (!"TABLE_CREATED".equals(eventType)) {
            return Uni.createFrom().voidItem(); // TABLE_DROPPED etc. — see class Javadoc
        }

        String namespacePath = node.path("namespacePath").asText(null);
        String tableName = node.path("tableName").asText(null);
        if (namespacePath == null || tableName == null) {
            LOG.warnf("TABLE_CREATED event missing namespacePath/tableName: %s", rawJson);
            return Uni.createFrom().voidItem();
        }

        String datasetPath = namespacePath + "." + tableName;
        UpsertEmbeddingRequest request = new UpsertEmbeddingRequest(
                datasetPath, null, EmbeddingTargetType.TABLE,
                "Table " + tableName + " in namespace " + namespacePath);

        return embeddingService.upsert(request)
                .invoke(r -> LOG.infof("Auto-embedded new table %s", datasetPath))
                .onFailure().invoke(t -> LOG.errorf(t, "Failed to auto-embed table %s", datasetPath))
                .replaceWithVoid();
    }
}
