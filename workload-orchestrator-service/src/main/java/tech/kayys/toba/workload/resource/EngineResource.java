package com.dremioplatform.workload.resource;

import com.dremioplatform.workload.service.EngineService;
import com.dremioplatform.workload.service.dto.CreateEngineRequest;
import com.dremioplatform.workload.service.dto.EngineResponse;
import com.dremioplatform.workload.service.dto.EngineScalingEventResponse;
import com.dremioplatform.workload.service.dto.ScaleEngineRequest;
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

@Path("/api/v1/engines")
@Tag(name = "Engines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EngineResource {

    @Inject EngineService engineService;

    @POST
    public Uni<Response> create(@Valid CreateEngineRequest request) {
        return engineService.create(request)
                .map(engine -> Response.status(Response.Status.CREATED).entity(engine).build());
    }

    @GET
    @Path("/{id}")
    public Uni<EngineResponse> getById(@PathParam("id") UUID id) {
        return engineService.getById(id);
    }

    @GET
    public Uni<List<EngineResponse>> listAll() {
        return engineService.listAll();
    }

    @PUT
    @Path("/{id}/start")
    @Operation(summary = "Start an engine (scales to max(minReplicas, 1))")
    public Uni<EngineResponse> start(@PathParam("id") UUID id, @HeaderParam("X-Actor-Id") UUID actorId) {
        return engineService.start(id, actorId);
    }

    @PUT
    @Path("/{id}/stop")
    @Operation(summary = "Stop an engine (scales to 0)")
    public Uni<EngineResponse> stop(@PathParam("id") UUID id, @HeaderParam("X-Actor-Id") UUID actorId) {
        return engineService.stop(id, actorId);
    }

    @PUT
    @Path("/{id}/scale")
    @Operation(summary = "Scale an engine to a specific replica count")
    public Uni<EngineResponse> scale(@PathParam("id") UUID id, @Valid ScaleEngineRequest request,
                                       @HeaderParam("X-Actor-Id") UUID actorId) {
        return engineService.scale(id, request, actorId);
    }

    @GET
    @Path("/{id}/scaling-history")
    public Uni<List<EngineScalingEventResponse>> scalingHistory(@PathParam("id") UUID id,
                                                                   @QueryParam("limit") Integer limit) {
        return engineService.scalingHistory(id, limit == null ? 50 : limit);
    }
}
