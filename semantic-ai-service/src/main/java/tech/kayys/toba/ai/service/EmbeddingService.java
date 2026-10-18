package com.dremioplatform.ai.service;

import com.dremioplatform.ai.domain.DatasetEmbedding;
import com.dremioplatform.ai.domain.EmbeddingTargetType;
import com.dremioplatform.ai.llm.LlmClient;
import com.dremioplatform.ai.repository.DatasetEmbeddingRepository;
import com.dremioplatform.ai.service.dto.EmbeddingResponse;
import com.dremioplatform.ai.service.dto.SemanticSearchResult;
import com.dremioplatform.ai.service.dto.UpsertEmbeddingRequest;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EmbeddingService {

    @Inject DatasetEmbeddingRepository embeddingRepository;
    @Inject LlmClient llmClient;

    /**
     * Upserts an embedding for one dataset/column/wiki target. Two-step by
     * necessity (see DatasetEmbedding's Javadoc on why the vector isn't JPA-mapped):
     * embed the text first (outside the transaction — an LLM call has no business
     * holding a DB transaction open), then persist/update the row and write the
     * vector column via native SQL in the same transaction.
     */
    @WithTransaction
    public Uni<EmbeddingResponse> upsert(UpsertEmbeddingRequest request) {
        float[] vector = llmClient.embed(request.sourceText());
        String model = llmClient.embeddingModelName();

        return embeddingRepository.findByTarget(request.datasetPath(), request.columnName(), request.targetType())
                .chain(existing -> {
                    DatasetEmbedding entity = existing != null ? existing : new DatasetEmbedding();
                    boolean isNew = existing == null;
                    if (isNew) {
                        entity.id = UUID.randomUUID();
                        entity.datasetPath = request.datasetPath();
                        entity.columnName = request.columnName();
                        entity.targetType = request.targetType();
                    }
                    entity.sourceText = request.sourceText();
                    entity.embeddingModel = model;
                    entity.updatedAt = Instant.now();

                    Uni<DatasetEmbedding> saved = isNew ? embeddingRepository.persist(entity) : Uni.createFrom().item(entity);
                    return saved
                            .chain(e -> embeddingRepository.writeEmbeddingVector(e.id, vector).replaceWith(e))
                            .map(EmbeddingResponse::from);
                });
    }

    public Uni<List<EmbeddingResponse>> listForDataset(String datasetPath) {
        return embeddingRepository.findByDatasetPath(datasetPath)
                .map(list -> list.stream().map(EmbeddingResponse::from).toList());
    }

    /** Embeds the query text with the same model/dimension as stored embeddings
     *  (this is why {@link LlmClient#embeddingModelName()} must stay consistent —
     *  see the migration's comment on why changing embedding models needs a
     *  re-embed-everything pass, not just a config flip) and returns the nearest
     *  neighbors by cosine similarity. */
    public Uni<List<SemanticSearchResult>> search(String query, EmbeddingTargetType targetType, int limit) {
        float[] queryVector = llmClient.embed(query);
        return embeddingRepository.findSimilar(queryVector, limit, targetType)
                .map(list -> list.stream().map(SemanticSearchResult::from).toList());
    }
}
