package com.dremioplatform.ai.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * An AI-proposed table/column description (plan Section 5.5, "AI-generated wikis &
 * labels ... stored as suggestions for human approval, keeps humans in the loop,
 * avoids silently mutating metadata"). Nothing in this service ever writes a
 * suggestion's text into Dremio's actual wiki/label metadata — that write, if it
 * happens at all, is a human action taken after reviewing `suggestedDescription`
 * here (via the not-yet-built admin UI or a direct call to Dremio's own wiki API,
 * outside this service's scope).
 */
@Entity
@Table(name = "wiki_suggestion")
public class WikiSuggestion extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String datasetPath;

    /** Null for a table-level description suggestion. */
    public String columnName;

    @Column(nullable = false, columnDefinition = "text")
    public String suggestedDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public WikiSuggestionStatus status = WikiSuggestionStatus.PENDING;

    @Column(nullable = false)
    public String generatedByModel;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public UUID reviewedByUserId;
    public Instant reviewedAt;

    /** Free-text reviewer note, e.g. why a suggestion was rejected — useful signal
     *  for prompt-tuning later, even though nothing consumes it programmatically yet. */
    public String reviewNote;
}
