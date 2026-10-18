package com.dremioplatform.ai.llm;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Real embedding provider via `quarkus-langchain4j-openai`'s auto-configured
 * {@link EmbeddingModel} bean (model: `text-embedding-3-small`, 1536 dimensions —
 * matching {@code dataset_embedding.embedding}'s pinned `vector(1536)` column, see
 * the migration's comment on why that dimension is fixed). Used when
 * `ai.embedding.provider=openai`, independent of `ai.mock-mode` (which only
 * controls chat) — see {@link RoutingLlmClient}'s Javadoc for how the two flags
 * combine.
 *
 * This is a genuinely different vendor from the chat provider (Anthropic), which
 * is fine and expected — see {@link LlmClient}'s Javadoc for why Anthropic can't
 * supply embeddings itself. `quarkus.langchain4j.openai.chat-model.enabled=false`
 * in application.properties prevents OpenAI's own chat model from also registering
 * a {@code ChatLanguageModel} bean and creating an ambiguous injection point
 * alongside Anthropic's.
 */
@ApplicationScoped
public class OpenAiEmbeddingClient {

    @Inject EmbeddingModel embeddingModel;

    @ConfigProperty(name = "quarkus.langchain4j.openai.embedding-model.model-name")
    String modelName;

    public float[] embed(String text) {
        Response<Embedding> response = embeddingModel.embed(TextSegment.from(text));
        return response.content().vector();
    }

    public String modelName() {
        return modelName;
    }
}
