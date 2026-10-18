package com.dremioplatform.ai.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Real chat completion via `quarkus-langchain4j-anthropic` — the extension
 * auto-configures a {@link ChatLanguageModel} CDI bean from
 * `quarkus.langchain4j.anthropic.*` config (see application.properties). Used for
 * AI_SUMMARIZE/AI_CLASSIFY and wiki-suggestion generation when `ai.mock-mode=false`.
 *
 * Does NOT implement embeddings — see {@link LlmClient}'s Javadoc for why
 * (Anthropic has no public embeddings API). {@link #embed} throws rather than
 * silently returning something wrong.
 */
@ApplicationScoped
public class AnthropicChatClient {

    @Inject ChatLanguageModel chatLanguageModel;

    @ConfigProperty(name = "quarkus.langchain4j.anthropic.chat-model.model-name")
    String modelName;

    public LlmClient.ChatResult chat(String systemPrompt, String userPrompt) {
        Response<AiMessage> response = chatLanguageModel.generate(
                List.of(SystemMessage.from(systemPrompt), UserMessage.from(userPrompt)));
        var usage = response.tokenUsage();
        return new LlmClient.ChatResult(
                response.content().text(),
                usage == null ? null : usage.inputTokenCount(),
                usage == null ? null : usage.outputTokenCount());
    }

    public String chatModelName() {
        return modelName;
    }
}
