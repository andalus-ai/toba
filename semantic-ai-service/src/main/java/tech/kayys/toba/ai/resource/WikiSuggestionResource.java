package com.dremioplatform.ai.resource;

import com.dremioplatform.ai.service.WikiSuggestionService;
import com.dremioplatform.ai.service.dto.GenerateWikiSuggestionRequest;
import com.dremioplatform.ai.service.dto.ReviewWikiSuggestionRequest;
import com.dremioplatform.ai.service.dto.WikiSuggestionResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/wiki-suggestions")
@Tag(name = "Wiki Suggestions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WikiSuggestionResource {

    @Inject WikiSuggestionService wikiSuggestionService;

    @POST
    @Operation(summary = "Generate an AI description suggestion for a table/column (stored as PENDING, never auto-applied)")
    public Uni<Response> generate(@Valid GenerateWikiSuggestionRequest request) {
        return wikiSuggestionService.generate(request)
                .map(s -> Response.status(Response.Status.CREATED).entity(s).build());
    }

    @PUT
    @Path("/{id}/review")
    @Operation(summary = "Approve or reject a pending suggestion")
    public Uni<WikiSuggestionResponse> review(@PathParam("id") UUID id, @Valid ReviewWikiSuggestionRequest request,
                                                @HeaderParam("X-Actor-Id") UUID actorId) {
        return wikiSuggestionService.review(id, request, actorId);
    }

    @GET
    @Path("/pending")
    public Uni<List<WikiSuggestionResponse>> listPending(@QueryParam("limit") Integer limit) {
        return wikiSuggestionService.listPending(limit == null ? 50 : limit);
    }

    @GET
    public Uni<List<WikiSuggestionResponse>> listForDataset(@QueryParam("datasetPath") String datasetPath) {
        return wikiSuggestionService.listForDataset(datasetPath);
    }
}
