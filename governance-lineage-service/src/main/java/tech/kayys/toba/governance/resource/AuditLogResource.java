package com.dremioplatform.governance.resource;

import com.dremioplatform.governance.service.AuditQueryService;
import com.dremioplatform.governance.service.dto.AuditLogEntryResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/audit-log")
@Tag(name = "Audit Log")
@Produces(MediaType.APPLICATION_JSON)
public class AuditLogResource {

    @Inject AuditQueryService auditQueryService;

    @GET
    @Path("/by-resource")
    @Operation(summary = "Compliance evidence query: everything that happened to a resource path in a time range")
    public Uni<List<AuditLogEntryResponse>> byResource(@QueryParam("resourcePath") String resourcePath,
                                                          @QueryParam("since") Instant since,
                                                          @QueryParam("until") Instant until) {
        return auditQueryService.byResource(resourcePath, since, until);
    }

    @GET
    @Path("/by-actor")
    public Uni<List<AuditLogEntryResponse>> byActor(@QueryParam("actorPrincipalType") String actorPrincipalType,
                                                       @QueryParam("actorPrincipalId") String actorPrincipalId,
                                                       @QueryParam("since") Instant since,
                                                       @QueryParam("until") Instant until) {
        return auditQueryService.byActor(actorPrincipalType, actorPrincipalId, since, until);
    }
}
