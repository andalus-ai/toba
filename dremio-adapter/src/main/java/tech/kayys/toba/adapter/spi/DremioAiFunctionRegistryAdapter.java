package com.dremioplatform.adapter.spi;

import com.dremioplatform.adapter.ai.AiFunctionHttpClient;
import java.util.List;

/**
 * Implements the mirror {@link DremioAiFunctionRegistry} by delegating to
 * {@link AiFunctionHttpClient}. See that class's and {@link DremioAiFunctionRegistry}'s
 * Javadoc, and RECONCILIATION_NOTES.md's Finding 3, before wiring this into a real
 * `SimpleFunction` — every method here is a blocking network call, on purpose,
 * because that's what the real per-row execution contract requires.
 */
public class DremioAiFunctionRegistryAdapter implements DremioAiFunctionRegistry {

    private final AiFunctionHttpClient aiFunctionHttpClient;

    public DremioAiFunctionRegistryAdapter(AiFunctionHttpClient aiFunctionHttpClient) {
        this.aiFunctionHttpClient = aiFunctionHttpClient;
    }

    @Override
    public String summarize(String inputText) {
        return aiFunctionHttpClient.summarize(inputText);
    }

    @Override
    public String classify(String inputText, List<String> categories) {
        return aiFunctionHttpClient.classify(inputText, categories);
    }
}
