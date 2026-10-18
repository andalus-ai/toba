package com.dremioplatform.ai.resource;

import com.dremioplatform.ai.domain.EmbeddingTargetType;
import com.dremioplatform.ai.service.EmbeddingService;
import com.dremioplatform.ai.service.dto.EmbeddingResponse;
import com.dremioplatform.ai.service.dto.SemanticSearchRequest;
import com.dremioplatform.ai.service.dto.SemanticSearchResult;
import com.dremioplatform.ai.service.dto.UpsertEmbeddingRequest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/embeddings")
@Tag(name = "Embeddings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmbeddingResource {

    @Inject EmbeddingService embeddingService;

    @POST
    @Operation(summary = "Create or update an embedding for a table/column/wiki")
    public Uni<Response> upsert(@Valid UpsertEmbeddingRequest request) {
        return embeddingService.upsert(request)
                .map(e -> Response.status(Response.Status.OK).entity(e).build());
    }

    @GET
    public Uni<List<EmbeddingResponse>> listForDataset(@QueryParam("datasetPath") String datasetPath) {
        return embeddingService.listForDataset(datasetPath);
    }

    @POST
    @Path("/search")
    @Operation(summary = "Semantic search over embedded tables/columns/wikis")
    public Uni<List<SemanticSearchResult>> search(@Valid SemanticSearchRequest request) {
        EmbeddingTargetType targetType = request.targetType();
        int limit = request.limit() == null ? 10 : request.limit();
        return embeddingService.search(request.query(), targetType, limit);
    }
}
