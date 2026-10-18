package com.dremioplatform.adapter.http;

import com.dremioplatform.adapter.config.AdapterConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls governance-lineage-service's REST {@code GET /api/v1/masking-policies/applicable}
 * endpoint — plain REST rather than gRPC because that endpoint isn't on the plan's
 * gRPC list yet (see that service's README: "No gRPC masking-policy lookup yet").
 * A short-TTL cache absorbs the REST round trip's extra latency versus a gRPC call,
 * since this also sits on the query-plan hot path (masking must be evaluated before
 * every query against a masked dataset commits to a plan).
 *
 * Response parsing here is a minimal hand-rolled JSON reader (regex-based field
 * extraction) rather than a JSON library, to keep this jar's dependency footprint
 * minimal on Dremio's classpath (see module README on why Quarkus/Jackson aren't
 * pulled in here). Swap for a real JSON library if/when the adapter's dependency
 * budget allows it — this is a known simplification, not a design endorsement.
 */
public class MaskingPolicyHttpClient {

    private static final Logger LOG = Logger.getLogger(MaskingPolicyHttpClient.class.getName());
    private static final Pattern EXPRESSION_FIELD = Pattern.compile("\"expression\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern COLUMN_FIELD = Pattern.compile("\"columnName\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"|\"columnName\"\\s*:\\s*null");
    private static final Pattern RULE_TYPE_FIELD = Pattern.compile("\"ruleType\"\\s*:\\s*\"(COLUMN_MASK|ROW_FILTER)\"");

    private final HttpClient httpClient;
    private final String governanceBaseUrl;
    private final long cacheTtlMillis;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public MaskingPolicyHttpClient(AdapterConfig config) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();
        this.governanceBaseUrl = config.governanceBaseUrl();
        this.cacheTtlMillis = config.maskingPolicyCacheTtlMillis();
    }

    /** Fails OPEN to an empty list (no masking applied) is NOT acceptable here —
     *  unlike authz, an unreachable Governance Service must not silently expose
     *  unmasked sensitive data. Callers (see {@code spi/} adapters) must treat a
     *  thrown {@link MaskingPolicyLookupException} as "block the query", not
     *  "proceed unmasked" — the opposite fail-direction from AuthCheckGrpcClient. */
    public List<MaskingRule> findApplicable(String scopePath) {
        CacheEntry cached = cache.get(scopePath);
        if (cached != null && cached.isFresh(cacheTtlMillis)) {
            return cached.rules;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(governanceBaseUrl + "/api/v1/masking-policies/applicable?scopePath="
                            + java.net.URLEncoder.encode(scopePath, java.nio.charset.StandardCharsets.UTF_8)))
                    .timeout(Duration.ofMillis(1000))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new MaskingPolicyLookupException("Governance Service returned HTTP " + response.statusCode());
            }
            List<MaskingRule> rules = parseRules(response.body());
            cache.put(scopePath, new CacheEntry(rules, System.currentTimeMillis()));
            return rules;
        } catch (MaskingPolicyLookupException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Masking-policy lookup failed for scopePath=" + scopePath, e);
            throw new MaskingPolicyLookupException("Failed to reach Governance Service", e);
        }
    }

    private static List<MaskingRule> parseRules(String jsonArray) {
        // Minimal splitter: the response is a JSON array of flat objects (no nested
        // arrays/objects inside each policy other than exemptRoleIds, which this
        // parser deliberately ignores — exemption enforcement is not yet wired into
        // the adapter side, see module README "Known gaps").
        List<MaskingRule> rules = new java.util.ArrayList<>();
        for (String obj : splitTopLevelObjects(jsonArray)) {
            Matcher exprMatcher = EXPRESSION_FIELD.matcher(obj);
            Matcher ruleTypeMatcher = RULE_TYPE_FIELD.matcher(obj);
            if (exprMatcher.find() && ruleTypeMatcher.find()) {
                Matcher colMatcher = COLUMN_FIELD.matcher(obj);
                String column = colMatcher.find() ? colMatcher.group(1) : null;
                rules.add(new MaskingRule(ruleTypeMatcher.group(1), column, unescape(exprMatcher.group(1))));
            }
        }
        return rules;
    }

    private static List<String> splitTopLevelObjects(String jsonArray) {
        List<String> objects = new java.util.ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(jsonArray.substring(start, i + 1));
                }
            }
        }
        return objects;
    }

    private static String unescape(String raw) {
        return raw.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public record MaskingRule(String ruleType, String columnName, String expression) {
    }

    public static class MaskingPolicyLookupException extends RuntimeException {
        public MaskingPolicyLookupException(String message) {
            super(message);
        }

        public MaskingPolicyLookupException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record CacheEntry(List<MaskingRule> rules, long fetchedAtMillis) {
        boolean isFresh(long ttlMillis) {
            return System.currentTimeMillis() - fetchedAtMillis < ttlMillis;
        }
    }
}
