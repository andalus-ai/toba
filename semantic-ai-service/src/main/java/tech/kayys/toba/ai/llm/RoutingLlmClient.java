package com.dremioplatform.ai.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The only {@link LlmClient} bean in this service — {@link MockLlmClient},
 * {@link AnthropicChatClient}, and {@link OpenAiEmbeddingClient} are plain
 * (non-{@code LlmClient}) beans this class delegates to internally, so there's no
 * CDI ambiguity for anything that injects {@code LlmClient} elsewhere.
 *
 * Two independent config flags control routing:
 * - {@code ai.mock-mode} controls {@code chat()}: {@code true} -> {@link MockLlmClient};
 *   {@code false} -> {@link AnthropicChatClient} (real call — costs money, needs
 *   {@code ANTHROPIC_API_KEY}).
 * - {@code ai.embedding.provider} controls {@code embed()} independently:
 *   {@code "mock"} (default) -> {@link MockLlmClient}'s deterministic pseudo-vector;
 *   {@code "openai"} -> {@link OpenAiEmbeddingClient} (real call — costs money,
 *   needs {@code OPENAI_API_KEY}). These are deliberately separate flags, not one
 *   "mock everything" switch — chat and embeddings can be real independently, and
 *   a real embedding provider from a *different* vendor than the chat provider
 *   (OpenAI embeddings + Anthropic chat) is expected here, not a bug, since
 *   Anthropic has no embeddings API at all (see {@link LlmClient}'s Javadoc).
 *
 * IMPORTANT: switching {@code ai.embedding.provider} after any data has already
 * been embedded requires re-embedding everything that used the old provider —
 * vectors from different models/providers are not comparable, and cosine-distance
 * search across mixed-provider vectors in the same table produces meaningless
 * results. See the migration's comment on the pinned `vector(1536)` dimension for
 * the same reason stated from the schema side.
 */
@ApplicationScoped
public class RoutingLlmClient implements LlmClient {

    private static final Logger LOG = Logger.getLogger(RoutingLlmClient.class);

    @Inject MockLlmClient mockLlmClient;
    @Inject AnthropicChatClient anthropicChatClient;
    @Inject OpenAiEmbeddingClient openAiEmbeddingClient;

    @ConfigProperty(name = "ai.mock-mode")
    boolean mockMode;

    @ConfigProperty(name = "ai.embedding.provider")
    String embeddingProvider;

    @Override
    public ChatResult chat(String systemPrompt, String userPrompt) {
        return mockMode ? mockLlmClient.chat(systemPrompt, userPrompt) : anthropicChatClient.chat(systemPrompt, userPrompt);
    }

    @Override
    public float[] embed(String text) {
        return isRealEmbeddingProvider() ? openAiEmbeddingClient.embed(text) : mockLlmClient.embed(text);
    }

    @Override
    public String chatModelName() {
        return mockMode ? mockLlmClient.chatModelName() : anthropicChatClient.chatModelName();
    }

    @Override
    public String embeddingModelName() {
        return isRealEmbeddingProvider() ? openAiEmbeddingClient.modelName() : mockLlmClient.embeddingModelName();
    }

    private boolean isRealEmbeddingProvider() {
        return "openai".equalsIgnoreCase(embeddingProvider);
    }
}
