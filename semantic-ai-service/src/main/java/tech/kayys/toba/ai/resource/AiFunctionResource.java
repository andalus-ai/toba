package com.dremioplatform.ai.resource;

import com.dremioplatform.ai.service.AiFunctionService;
import com.dremioplatform.ai.service.dto.AiFunctionRequest;
import com.dremioplatform.ai.service.dto.AiFunctionResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * The endpoint the not-yet-built Dremio UDF proxies each row's input to — see
 * AiFunctionService's Javadoc for the batching caveat on doing this per-row over a
 * large result set.
 */
@Path("/api/v1/ai-functions")
@Tag(name = "AI Functions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AiFunctionResource {

    @Inject AiFunctionService aiFunctionService;

    @POST
    @Path("/summarize")
    @Operation(summary = "AI_SUMMARIZE backend — one-sentence summary of the input text")
    public Uni<AiFunctionResponse> summarize(@Valid AiFunctionRequest request,
                                               @HeaderParam("X-Principal-Type") String principalType,
                                               @HeaderParam("X-Principal-Id") String principalId) {
        return aiFunctionService.summarize(request, principalType, principalId);
    }

    @POST
    @Path("/classify")
    @Operation(summary = "AI_CLASSIFY backend — picks the best-fit category from a caller-supplied list")
    public Uni<AiFunctionResponse> classify(@Valid AiFunctionRequest request,
                                              @HeaderParam("X-Principal-Type") String principalType,
                                              @HeaderParam("X-Principal-Id") String principalId) {
        return aiFunctionService.classify(request, principalType, principalId);
    }
}
