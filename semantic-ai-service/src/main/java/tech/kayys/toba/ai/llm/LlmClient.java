package com.dremioplatform.ai.llm;

/**
 * The platform's "bring your own model" seam (plan Section 5.5: "pluggable LLM
 * provider (OpenAI/Anthropic/Gemini/Bedrock, 'bring your own model' like
 * Enterprise)"). Every AI-function/wiki-suggestion/embedding call in this service
 * goes through this interface, never a provider SDK directly — swapping providers
 * means writing a new implementation and changing which one {@link RoutingLlmClient}
 * delegates to, not touching any service-layer code.
 *
 * PROVIDERS ARE MIXED ON PURPOSE: chat completion is real via Anthropic
 * ({@link AnthropicChatClient}); embeddings are real via OpenAI
 * ({@link OpenAiEmbeddingClient}), controlled by a separate config flag
 * (`ai.embedding.provider`). This isn't an inconsistency — Anthropic does not
 * publish a public embeddings API at all, so there is no "AnthropicEmbeddingClient"
 * that could exist. Using a different vendor for embeddings than for chat is the
 * normal, expected shape of a "bring your own model" setup, not a workaround.
 *
 * Mock implementations of both capabilities exist ({@link MockLlmClient}) for
 * local dev/tests without any API key or cost — see {@link RoutingLlmClient}'s
 * Javadoc for exactly how `ai.mock-mode` (chat) and `ai.embedding.provider`
 * (embeddings) independently select real-vs-mock for each capability.
 */
public interface LlmClient {

    ChatResult chat(String systemPrompt, String userPrompt);

    float[] embed(String text);

    String chatModelName();

    String embeddingModelName();

    record ChatResult(String text, Integer promptTokens, Integer completionTokens) {
    }
}
