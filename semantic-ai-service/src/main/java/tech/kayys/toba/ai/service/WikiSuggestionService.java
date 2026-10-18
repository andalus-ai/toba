package com.dremioplatform.ai.service;

import com.dremioplatform.ai.domain.WikiSuggestion;
import com.dremioplatform.ai.domain.WikiSuggestionStatus;
import com.dremioplatform.ai.llm.LlmClient;
import com.dremioplatform.ai.repository.WikiSuggestionRepository;
import com.dremioplatform.ai.service.dto.GenerateWikiSuggestionRequest;
import com.dremioplatform.ai.service.dto.ReviewWikiSuggestionRequest;
import com.dremioplatform.ai.service.dto.WikiSuggestionResponse;
import com.dremioplatform.common.error.ConflictException;
import com.dremioplatform.common.error.EntityNotFoundException;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * See {@link WikiSuggestion}'s Javadoc — this service NEVER writes a suggestion's
 * text into Dremio's real wiki/label metadata. {@link #generate} only produces and
 * stores a suggestion; {@link #review} only changes its status. Applying an
 * approved suggestion to Dremio is a deliberately separate, human-triggered action
 * outside this service's scope (plan Section 5.5's "keeps humans in the loop").
 */
@ApplicationScoped
public class WikiSuggestionService {

    private static final String SYSTEM_PROMPT = """
            You are a data catalog assistant. Given a table or column's schema and \
            sample-data context, write a concise (1-2 sentence) plain-English description \
            suitable for a data catalog wiki entry. Do not speculate about data you weren't \
            shown; describe only what the provided context supports.""";

    @Inject WikiSuggestionRepository wikiSuggestionRepository;
    @Inject LlmClient llmClient;

    @WithTransaction
    public Uni<WikiSuggestionResponse> generate(GenerateWikiSuggestionRequest request) {
        String userPrompt = "Dataset path: " + request.datasetPath()
                + (request.columnName() != null ? "\nColumn: " + request.columnName() : "")
                + "\n\nContext:\n" + request.context();

        LlmClient.ChatResult result = llmClient.chat(SYSTEM_PROMPT, userPrompt);

        WikiSuggestion suggestion = new WikiSuggestion();
        suggestion.id = UUID.randomUUID();
        suggestion.datasetPath = request.datasetPath();
        suggestion.columnName = request.columnName();
        suggestion.suggestedDescription = result.text();
        suggestion.generatedByModel = llmClient.chatModelName();

        return wikiSuggestionRepository.persist(suggestion).map(WikiSuggestionResponse::from);
    }

    @WithTransaction
    public Uni<WikiSuggestionResponse> review(UUID id, ReviewWikiSuggestionRequest request, UUID reviewerUserId) {
        return wikiSuggestionRepository.findById(id)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "AI-404-SUGGESTION-NOT-FOUND", "WikiSuggestion", id))
                .chain(suggestion -> {
                    if (suggestion.status != WikiSuggestionStatus.PENDING) {
                        return Uni.createFrom().failure(new ConflictException(
                                "AI-409-SUGGESTION-ALREADY-REVIEWED",
                                "Suggestion has already been " + suggestion.status));
                    }
                    suggestion.status = Boolean.TRUE.equals(request.approve())
                            ? WikiSuggestionStatus.APPROVED : WikiSuggestionStatus.REJECTED;
                    suggestion.reviewedByUserId = reviewerUserId;
                    suggestion.reviewedAt = Instant.now();
                    suggestion.reviewNote = request.reviewNote();
                    return Uni.createFrom().item(suggestion);
                })
                .map(WikiSuggestionResponse::from);
    }

    public Uni<List<WikiSuggestionResponse>> listPending(int limit) {
        return wikiSuggestionRepository.findByStatus(WikiSuggestionStatus.PENDING, limit)
                .map(list -> list.stream().map(WikiSuggestionResponse::from).toList());
    }

    public Uni<List<WikiSuggestionResponse>> listForDataset(String datasetPath) {
        return wikiSuggestionRepository.findByDatasetPath(datasetPath)
                .map(list -> list.stream().map(WikiSuggestionResponse::from).toList());
    }
}
