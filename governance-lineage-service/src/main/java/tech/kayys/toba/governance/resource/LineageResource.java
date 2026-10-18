package com.dremioplatform.governance.resource;

import com.dremioplatform.governance.service.LineageService;
import com.dremioplatform.governance.service.dto.CreateLineageEdgeRequest;
import com.dremioplatform.governance.service.dto.LineageEdgeResponse;
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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/lineage")
@Tag(name = "Lineage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LineageResource {

    @Inject LineageService lineageService;

    @POST
    @Path("/edges")
    public Uni<Response> recordEdge(@Valid CreateLineageEdgeRequest request) {
        return lineageService.recordEdge(request)
                .map(edge -> Response.status(Response.Status.CREATED).entity(edge).build());
    }

    @GET
    @Path("/upstream")
    public Uni<List<LineageEdgeResponse>> upstream(@QueryParam("path") String path) {
        return lineageService.upstreamOf(path);
    }

    @GET
    @Path("/downstream")
    public Uni<List<LineageEdgeResponse>> downstream(@QueryParam("path") String path) {
        return lineageService.downstreamOf(path);
    }
}
