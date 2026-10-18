package com.dremioplatform.adapter.ai;

import com.dremioplatform.adapter.config.AdapterConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Calls semantic-ai-service's real, already-built {@code POST /api/v1/ai-functions/summarize}
 * and {@code /classify} endpoints. See {@link com.dremioplatform.adapter.spi.DremioAiFunctionRegistry}'s
 * Javadoc and RECONCILIATION_NOTES.md's Finding 3 before wiring this into a real
 * `SimpleFunction.eval()` — this client is synchronous/blocking BY DESIGN, since
 * that's what the real per-row execution contract requires; that is a documented
 * constraint here, not an oversight.
 *
 * A response cache keyed by SHA-256(functionName + inputText [+ categories]) is
 * included because real datasets frequently have highly repeated categorical text
 * (support-ticket categories, product types, sentiment-bearing short strings) —
 * this turns "N rows with the same value" into one real LLM call plus N cheap
 * cache hits, which matters a lot given how expensive the alternative (N blocking
 * network calls) is on the execution thread. It does NOT solve the fundamental
 * scale problem for high-cardinality columns — see the class-level warning in
 * {@code DremioAiFunctionRegistry} about batch/ETL-scale use.
 *
 * Same minimal-dependency, hand-rolled-JSON-parsing approach as
 * {@code MaskingPolicyHttpClient} — see that class's Javadoc for why (this jar's
 * classpath footprint inside Dremio's JVM is deliberately kept small).
 */
public class AiFunctionHttpClient {

    private static final Logger LOG = Logger.getLogger(AiFunctionHttpClient.class.getName());
    private static final Pattern RESULT_FIELD = Pattern.compile("\"result\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final HttpClient httpClient;
    private final String semanticAiBaseUrl;
    private final long timeoutMillis;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public AiFunctionHttpClient(AdapterConfig config) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();
        this.semanticAiBaseUrl = config.semanticAiBaseUrl();
        this.timeoutMillis = config.aiFunctionTimeoutMillis();
    }

    public String summarize(String inputText) {
        String cacheKey = cacheKey("AI_SUMMARIZE", inputText, null);
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String body = "{\"inputText\":" + jsonString(inputText) + "}";
        String result = callAndExtractResult("/api/v1/ai-functions/summarize", body);
        cache.put(cacheKey, result);
        return result;
    }

    public String classify(String inputText, List<String> categories) {
        String cacheKey = cacheKey("AI_CLASSIFY", inputText, categories);
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String categoriesJson = categories.stream()
                .map(this::jsonString)
                .collect(Collectors.joining(",", "[", "]"));
        String body = "{\"inputText\":" + jsonString(inputText) + ",\"categories\":" + categoriesJson + "}";
        String result = callAndExtractResult("/api/v1/ai-functions/classify", body);
        cache.put(cacheKey, result);
        return result;
    }

    private String callAndExtractResult(String path, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(semanticAiBaseUrl + path))
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AiFunctionCallException("semantic-ai-service returned HTTP " + response.statusCode());
            }
            Matcher matcher = RESULT_FIELD.matcher(response.body());
            if (!matcher.find()) {
                throw new AiFunctionCallException("Could not parse 'result' field from response");
            }
            return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
        } catch (AiFunctionCallException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "AI function call to " + path + " failed", e);
            throw new AiFunctionCallException("Failed to call semantic-ai-service " + path, e);
        }
    }

    private static String cacheKey(String functionName, String inputText, List<String> categories) {
        try {
            String raw = functionName + "\u0000" + inputText + "\u0000" + (categories == null ? "" : String.join(",", categories));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String jsonString(String raw) {
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public static class AiFunctionCallException extends RuntimeException {
        public AiFunctionCallException(String message) {
            super(message);
        }

        public AiFunctionCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
