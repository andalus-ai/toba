package com.dremioplatform.governance.resource;

import com.dremioplatform.governance.service.MaskingPolicyService;
import com.dremioplatform.governance.service.dto.CreateMaskingPolicyRequest;
import com.dremioplatform.governance.service.dto.MaskingPolicyResponse;
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

@Path("/api/v1/masking-policies")
@Tag(name = "Masking Policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaskingPolicyResource {

    @Inject MaskingPolicyService maskingPolicyService;

    @POST
    public Uni<Response> create(@Valid CreateMaskingPolicyRequest request, @HeaderParam("X-Actor-Id") UUID actorId) {
        return maskingPolicyService.create(request, actorId)
                .map(policy -> Response.status(Response.Status.CREATED).entity(policy).build());
    }

    @PUT
    @Path("/{id}/disable")
    public Uni<MaskingPolicyResponse> disable(@PathParam("id") UUID id) {
        return maskingPolicyService.disable(id);
    }

    @GET
    public Uni<List<MaskingPolicyResponse>> listAll() {
        return maskingPolicyService.listAll();
    }

    @GET
    @Path("/applicable")
    @Operation(summary = "Policies applicable to a scope path (itself or any ancestor) — the dremio-adapter's lookup call")
    public Uni<List<MaskingPolicyResponse>> applicable(@QueryParam("scopePath") String scopePath) {
        return maskingPolicyService.findApplicable(scopePath);
    }
}
