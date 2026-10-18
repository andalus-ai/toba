package com.dremioplatform.ai.mcp;

import com.dremioplatform.ai.domain.EmbeddingTargetType;
import com.dremioplatform.ai.service.EmbeddingService;
import com.dremioplatform.ai.service.dto.EmbeddingResponse;
import com.dremioplatform.ai.service.dto.SemanticSearchResult;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * REAL MCP tools, discovered and JSON-Schema-generated at build time by
 * `quarkus-mcp-server-http` from these `@Tool`/`@ToolArg` annotations — no manual
 * JSON-RPC wiring, no hand-written schema strings. Served over Streamable HTTP at
 * `/mcp` (legacy SSE at `/mcp/sse`), authenticated via platform JWTs (see
 * application.properties' `quarkus.http.auth.permission.mcp` rule).
 *
 * Both tools are now authorized per-call via {@link McpAuthorizationService},
 * reusing identity-access-service's real `AuthCheckService` gRPC contract — the
 * same one `dremio-adapter` calls — rather than building new authz logic here.
 * `search_datasets` can't gate the search itself (a free-text query has no single
 * scope path), so it post-filters results to only paths the caller may read;
 * `get_dataset_embeddings` gates up front since it always has one target path.
 *
 * Errors use {@code io.quarkiverse.mcp.server.ToolCallException} deliberately, not
 * this platform's own exception hierarchy — verified against the extension's real
 * error-handling docs: `@Tool` methods aren't JAX-RS resources, so
 * {@code AiExceptionMapper} never sees them; `ToolCallException` is the
 * framework's actual mechanism for converting a business-logic failure into a
 * proper failed MCP tool response with the message included.
 *
 * Closes the "MCP server / agent tools" half of plan Section 5.5. The "run
 * governed queries" half is still NOT here — see class-level note at the bottom.
 */
@ApplicationScoped
public class SemanticCatalogMcpTools {

    private static final String DATASET_READ_PERMISSION = "catalog.dataset.read";

    @Inject EmbeddingService embeddingService;
    @Inject McpAuthorizationService authorizationService;

    @Tool(description = "Semantic search over the data catalog's tables, columns, and wikis. "
            + "Returns the most relevant matches for a natural-language query, filtered to "
            + "only what the caller is authorized to read.")
    public Uni<List<SemanticSearchResult>> searchDatasets(
            @ToolArg(description = "Natural-language search query") String query,
            @ToolArg(description = "Optional filter: TABLE, COLUMN, or WIKI", required = false) String targetType,
            @ToolArg(description = "Max results (default 10)", required = false) Integer limit) {
        EmbeddingTargetType parsedType = parseTargetTypeOrNull(targetType);
        int effectiveLimit = limit == null ? 10 : limit;

        return embeddingService.search(query, parsedType, effectiveLimit)
                .chain(results -> {
                    if (results.isEmpty()) {
                        return Uni.createFrom().item(results);
                    }
                    List<String> paths = results.stream().map(SemanticSearchResult::datasetPath).distinct().toList();
                    return authorizationService.filterAuthorized(paths, DATASET_READ_PERMISSION)
                            .map(allowedPaths -> {
                                Set<String> allowedSet = Set.copyOf(allowedPaths);
                                return results.stream().filter(r -> allowedSet.contains(r.datasetPath())).toList();
                            });
                });
    }

    @Tool(description = "List every embedded object (table/column/wiki) for a given dataset path — "
            + "useful to see what context is available before asking a question about a dataset.")
    public Uni<List<EmbeddingResponse>> getDatasetEmbeddings(
            @ToolArg(description = "Full dot-joined dataset path, e.g. 'space.sales.orders'") String datasetPath) {
        return authorizationService.checkPermission(datasetPath, DATASET_READ_PERMISSION)
                .chain(allowed -> {
                    if (!allowed) {
                        return Uni.createFrom().failure(new ToolCallException(
                                "Not authorized to read " + datasetPath));
                    }
                    return embeddingService.listForDataset(datasetPath);
                });
    }

    private static EmbeddingTargetType parseTargetTypeOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return EmbeddingTargetType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ToolCallException("targetType must be one of TABLE, COLUMN, WIKI — got '" + raw + "'");
        }
    }

    // NOTE: "run governed queries" (the other half of plan Section 5.5's MCP
    // description) is intentionally not a tool here. That needs a tool that
    // submits a query through Dremio's coordinator respecting the same RBAC/
    // masking this platform enforces elsewhere — it belongs in whichever service
    // ends up owning query submission proxying (likely dremio-adapter or a new
    // piece calling through it), not semantic-ai-service. Adding it here would
    // give this service a query-execution responsibility it doesn't otherwise have
    // and duplicate authz logic that already lives in identity-access-service's
    // AuthCheckService — the same service McpAuthorizationService now calls for
    // the discovery tools above.
}
