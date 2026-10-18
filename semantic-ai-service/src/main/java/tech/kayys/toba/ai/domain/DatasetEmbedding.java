package com.dremioplatform.ai.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;

/**
 * A dataset/column/wiki embedding row. Deliberately does NOT map the `embedding`
 * pgvector column through JPA — Hibernate ORM has no native `vector` type, and
 * fighting a custom UserType for one column isn't worth it here. Instead:
 * this entity owns every other column via normal Panache persist/update, and
 * {@link com.dremioplatform.ai.repository.DatasetEmbeddingRepository} writes/reads
 * the `embedding` column with hand-written native SQL (`::vector` cast) in the same
 * transaction. {@link #embeddingPreview} is a transient, UI-debugging-only field —
 * never persisted — populated only by the repository's similarity-search path.
 */
@Entity
@Table(name = "dataset_embedding")
public class DatasetEmbedding extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String datasetPath;

    /** Null for TABLE/WIKI-level embeddings. */
    public String columnName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EmbeddingTargetType targetType;

    /** The text that was embedded — schema description, sample-value summary, or
     *  wiki content, depending on targetType. Kept alongside the vector so search
     *  results can show *why* something matched without a second lookup. */
    @Column(nullable = false, columnDefinition = "text")
    public String sourceText;

    @Column(nullable = false)
    public String embeddingModel;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant updatedAt = Instant.now();

    @Transient
    public Double similarityScore;
}
