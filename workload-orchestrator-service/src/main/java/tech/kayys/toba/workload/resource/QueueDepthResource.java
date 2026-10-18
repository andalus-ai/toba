package com.dremioplatform.workload.resource;

import com.dremioplatform.workload.service.QueueDepthService;
import com.dremioplatform.workload.service.dto.QueueDepthSampleResponse;
import com.dremioplatform.workload.service.dto.RecordQueueDepthSampleRequest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * The target shape for the not-yet-built job-submission routing hook — see
 * QueueDepthSample's Javadoc. Called directly for now.
 */
@Path("/api/v1/engines/{id}/queue-depth-samples")
@Tag(name = "Queue Depth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QueueDepthResource {

    @Inject QueueDepthService queueDepthService;

    @POST
    @Operation(summary = "Record a queue-depth sample for an engine, feeding ScaleUpScheduler's evaluation")
    public Uni<Response> recordSample(@PathParam("id") UUID engineId, @Valid RecordQueueDepthSampleRequest request) {
        return queueDepthService.recordSample(engineId, request)
                .map(sample -> Response.status(Response.Status.CREATED).entity(sample).build());
    }
}
