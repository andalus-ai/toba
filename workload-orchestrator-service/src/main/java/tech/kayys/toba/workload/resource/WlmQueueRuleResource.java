package com.dremioplatform.workload.resource;

import com.dremioplatform.workload.service.WlmQueueRuleService;
import com.dremioplatform.workload.service.dto.CreateWlmQueueRuleRequest;
import com.dremioplatform.workload.service.dto.WlmQueueRuleResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/wlm-queue-rules")
@Tag(name = "WLM Queue Rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WlmQueueRuleResource {

    @Inject WlmQueueRuleService wlmQueueRuleService;

    @POST
    public Uni<Response> create(@Valid CreateWlmQueueRuleRequest request) {
        return wlmQueueRuleService.create(request)
                .map(rule -> Response.status(Response.Status.CREATED).entity(rule).build());
    }

    @GET
    public Uni<List<WlmQueueRuleResponse>> listAll() {
        return wlmQueueRuleService.listAll();
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") UUID id) {
        return wlmQueueRuleService.delete(id).map(v -> Response.noContent().build());
    }

    @GET
    @Path("/resolve")
    @Operation(summary = "Resolve which engine a query tag routes to — the job-submission hook's lookup call")
    public Uni<Response> resolve(@QueryParam("queryTag") String queryTag) {
        return wlmQueueRuleService.resolveEngineFor(queryTag)
                .map(maybeEngineId -> maybeEngineId
                        .map(id -> Response.ok(java.util.Map.of("engineId", id)).build())
                        .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                                .entity(java.util.Map.of("message", "No matching rule for tag: " + queryTag)).build()));
    }
}
