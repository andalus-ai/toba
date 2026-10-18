package com.dremioplatform.identity.resource;

import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.service.PersonalAccessTokenService;
import com.dremioplatform.identity.service.dto.CreatePatRequest;
import com.dremioplatform.identity.service.dto.PatResponse;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/personal-access-tokens")
@Tag(name = "Personal Access Tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonalAccessTokenResource {

    @Inject PersonalAccessTokenService patService;

    @POST
    @Operation(summary = "Issue a new PAT. The raw token is returned exactly once in this response.")
    public Uni<Response> issue(@Valid CreatePatRequest request) {
        return patService.issue(request)
                .map(pat -> Response.status(Response.Status.CREATED).entity(pat).build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> revoke(@PathParam("id") UUID id) {
        return patService.revoke(id).map(v -> Response.noContent().build());
    }

    @GET
    @Path("/principal/{principalType}/{principalId}")
    @Operation(summary = "List a principal's tokens (metadata only — never the raw secret or hash)")
    public Uni<List<PatResponse>> listForPrincipal(@PathParam("principalType") PrincipalType principalType,
                                                     @PathParam("principalId") UUID principalId) {
        return patService.listForPrincipal(principalType, principalId);
    }
}
