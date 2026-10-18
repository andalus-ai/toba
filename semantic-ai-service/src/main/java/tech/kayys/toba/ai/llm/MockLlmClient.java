package com.dremioplatform.ai.llm;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * NOT REAL AI. Every method here is a deterministic function of its input —
 * useful for exercising the embedding-storage/similarity-search pipeline and the
 * wiki-suggestion/AI-function REST flows end-to-end without an API key or any real
 * inference cost, but the "semantic" in semantic search does not apply to these
 * vectors: {@link #embed} seeds a PRNG from the input text's SHA-256 hash and
 * generates dimension-length floats from it, so identical text always produces the
 * identical vector (needed so re-embedding an unchanged table is a no-op-equivalent,
 * and so tests can assert exact similarity-search results) — but *similar* text
 * does NOT produce *similar* vectors the way a real embedding model's would. Do
 * not use this in any environment where actual semantic search quality matters.
 */
@ApplicationScoped
public class MockLlmClient {

    @ConfigProperty(name = "ai.embedding.dimension")
    int dimension;

    @ConfigProperty(name = "ai.embedding.model-name")
    String embeddingModelName;

    public LlmClient.ChatResult chat(String systemPrompt, String userPrompt) {
        String canned = "[mock-llm-response] Given the input, a reasonable summary/classification "
                + "would be derived here. (systemPrompt=" + truncate(systemPrompt, 40)
                + ", userPrompt=" + truncate(userPrompt, 80) + ")";
        int promptTokens = estimateTokens(systemPrompt) + estimateTokens(userPrompt);
        int completionTokens = estimateTokens(canned);
        return new LlmClient.ChatResult(canned, promptTokens, completionTokens);
    }

    public float[] embed(String text) {
        long seed = sha256Seed(text);
        Random random = new Random(seed);
        float[] vector = new float[dimension];
        float normSq = 0f;
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) random.nextGaussian();
            normSq += vector[i] * vector[i];
        }
        // Normalize to unit length, matching real embedding models' convention —
        // cosine-distance search behaves sanely either way, but this keeps the mock
        // vectors' magnitude realistic for anyone eyeballing stored values.
        float norm = (float) Math.sqrt(normSq);
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    public String chatModelName() {
        return "mock-chat-v1";
    }

    public String embeddingModelName() {
        return embeddingModelName;
    }

    private static long sha256Seed(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes());
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (digest[i] & 0xFF);
            }
            return seed;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, text.length() / 4);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
