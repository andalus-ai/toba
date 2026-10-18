package com.dremioplatform.ai.service;

import com.dremioplatform.ai.domain.AiFunctionInvocation;
import com.dremioplatform.ai.llm.LlmClient;
import com.dremioplatform.ai.repository.AiFunctionInvocationRepository;
import com.dremioplatform.ai.service.dto.AiFunctionRequest;
import com.dremioplatform.ai.service.dto.AiFunctionResponse;
import com.dremioplatform.common.error.ValidationException;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;

/**
 * Backs the Dremio UDFs `AI_SUMMARIZE(text)` and `AI_CLASSIFY(text, categories)`
 * (plan Section 5.5). The UDF side (a Dremio function implementation that proxies
 * each row's input to this service over HTTP/gRPC) is NOT part of this service —
 * see module README "Known gaps": that's a `dremio-adapter`-side piece, and a
 * per-row network call for a UDF invoked over a large result set is a real
 * performance/cost concern this increment flags but doesn't solve (batching is the
 * likely answer, mirroring `AuthCheckService.CheckPermissions`' batching rationale).
 *
 * Every invocation is logged to {@link AiFunctionInvocation} — including failures,
 * with {@code failed=true} and the error message — regardless of principal, since
 * cost/usage tracking needs a complete record, not just successes.
 */
@ApplicationScoped
public class AiFunctionService {

    private static final String SUMMARIZE_SYSTEM_PROMPT =
            "You are a text summarization function. Given input text, return a single, "
                    + "concise sentence summarizing it. Return only the summary, no preamble.";

    private static final String CLASSIFY_SYSTEM_PROMPT =
            "You are a text classification function. Given input text and a list of candidate "
                    + "categories, return exactly one category from the list that best fits the text. "
                    + "Return only the category name, no preamble or punctuation.";

    @Inject LlmClient llmClient;
    @Inject AiFunctionInvocationRepository invocationRepository;

    @WithTransaction
    public Uni<AiFunctionResponse> summarize(AiFunctionRequest request, String principalType, String principalId) {
        return invoke("AI_SUMMARIZE", SUMMARIZE_SYSTEM_PROMPT, request.inputText(),
                request, principalType, principalId);
    }

    @WithTransaction
    public Uni<AiFunctionResponse> classify(AiFunctionRequest request, String principalType, String principalId) {
        if (request.categories() == null || request.categories().isEmpty()) {
            return Uni.createFrom().failure(new ValidationException(
                    "AI-400-CATEGORIES-REQUIRED", "AI_CLASSIFY requires a non-empty categories list"));
        }
        String userPrompt = "Text: " + request.inputText() + "\n\nCategories: " + String.join(", ", request.categories());
        return invoke("AI_CLASSIFY", CLASSIFY_SYSTEM_PROMPT, userPrompt, request, principalType, principalId);
    }

    private Uni<AiFunctionResponse> invoke(String functionName, String systemPrompt, String userPrompt,
                                             AiFunctionRequest request, String principalType, String principalId) {
        long start = System.currentTimeMillis();
        AiFunctionInvocation invocation = new AiFunctionInvocation();
        invocation.id = UUID.randomUUID();
        invocation.functionName = functionName;
        invocation.inputText = request.inputText();
        invocation.principalType = principalType;
        invocation.principalId = principalId;
        invocation.sourceDatasetPath = request.sourceDatasetPath();

        try {
            LlmClient.ChatResult result = llmClient.chat(systemPrompt, userPrompt);
            long latency = System.currentTimeMillis() - start;

            invocation.outputText = result.text();
            invocation.model = llmClient.chatModelName();
            invocation.promptTokens = result.promptTokens();
            invocation.completionTokens = result.completionTokens();
            invocation.latencyMillis = latency;

            return invocationRepository.persist(invocation)
                    .map(v -> new AiFunctionResponse(result.text(), invocation.model,
                            result.promptTokens(), result.completionTokens(), latency));
        } catch (RuntimeException e) {
            invocation.model = llmClient.chatModelName();
            invocation.failed = true;
            invocation.errorMessage = e.getMessage();
            invocation.latencyMillis = System.currentTimeMillis() - start;
            return invocationRepository.persist(invocation)
                    .chain(v -> Uni.createFrom().failure(e));
        }
    }

    public Uni<List<AiFunctionInvocation>> recentForPrincipal(String principalType, String principalId,
                                                                 java.time.Instant since, java.time.Instant until) {
        return invocationRepository.findByPrincipal(principalType, principalId, since, until, 200);
    }
}
