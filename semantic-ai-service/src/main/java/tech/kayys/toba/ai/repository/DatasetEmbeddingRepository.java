package com.dremioplatform.ai.repository;

import com.dremioplatform.ai.domain.DatasetEmbedding;
import com.dremioplatform.ai.domain.EmbeddingTargetType;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * See {@link DatasetEmbedding}'s Javadoc for why the `embedding` column is written
 * and read via native SQL rather than JPA field mapping.
 */
@ApplicationScoped
public class DatasetEmbeddingRepository implements PanacheRepositoryBase<DatasetEmbedding, UUID> {

    public Uni<DatasetEmbedding> findByTarget(String datasetPath, String columnName, EmbeddingTargetType targetType) {
        if (columnName == null) {
            return find("datasetPath = ?1 and columnName is null and targetType = ?2", datasetPath, targetType).firstResult();
        }
        return find("datasetPath = ?1 and columnName = ?2 and targetType = ?3", datasetPath, columnName, targetType).firstResult();
    }

    public Uni<List<DatasetEmbedding>> findByDatasetPath(String datasetPath) {
        return find("datasetPath", datasetPath).list();
    }

    /** Writes the vector for a row that's already been persisted via the normal
     *  Panache `persist`/`update` path — see the two-step pattern in EmbeddingService. */
    public Uni<Void> writeEmbeddingVector(UUID id, float[] embedding) {
        return getSession().chain(session -> session
                .createNativeQuery("update dataset_embedding set embedding = :vec, updated_at = now() where id = :id")
                .setParameter("vec", toVectorLiteral(embedding))
                .setParameter("id", id)
                .executeUpdate()
                .replaceWithVoid());
    }

    /**
     * Cosine-distance nearest-neighbor search (`<=>` is pgvector's cosine-distance
     * operator; smaller is more similar). Returns rows with {@code similarityScore}
     * populated as `1 - distance` (so higher = more similar, matching how search
     * results are usually presented) — every other field is read via the normal
     * entity columns in the same query, avoiding an N+1 to hydrate the rest of each
     * result row.
     */
    public Uni<List<DatasetEmbedding>> findSimilar(float[] queryEmbedding, int limit, EmbeddingTargetType filterType) {
        String typeFilter = filterType != null ? "and target_type = :targetType " : "";
        String sql = "select id, dataset_path, column_name, target_type, source_text, embedding_model, "
                + "created_at, updated_at, 1 - (embedding <=> :vec) as similarity "
                + "from dataset_embedding where true " + typeFilter
                + "order by embedding <=> :vec limit :limit";

        return getSession().chain(session -> {
            var query = session.createNativeQuery(sql, Object[].class)
                    .setParameter("vec", toVectorLiteral(queryEmbedding))
                    .setParameter("limit", limit);
            if (filterType != null) {
                query = query.setParameter("targetType", filterType.name());
            }
            return query.getResultList();
        }).map(rows -> rows.stream().map(this::mapRow).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private DatasetEmbedding mapRow(Object rowObj) {
        Object[] row = (Object[]) rowObj;
        DatasetEmbedding e = new DatasetEmbedding();
        e.id = (UUID) row[0];
        e.datasetPath = (String) row[1];
        e.columnName = (String) row[2];
        e.targetType = EmbeddingTargetType.valueOf((String) row[3]);
        e.sourceText = (String) row[4];
        e.embeddingModel = (String) row[5];
        e.createdAt = ((java.time.OffsetDateTime) row[6]).toInstant();
        e.updatedAt = ((java.time.OffsetDateTime) row[7]).toInstant();
        e.similarityScore = ((Number) row[8]).doubleValue();
        return e;
    }

    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
