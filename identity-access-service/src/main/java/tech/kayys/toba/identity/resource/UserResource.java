package com.dremioplatform.identity.resource;

import com.dremioplatform.identity.service.UserService;
import com.dremioplatform.security.IdentityManagementGuard;
import com.dremioplatform.identity.service.dto.CreateUserRequest;
import com.dremioplatform.identity.service.dto.UserResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Actor identity (who is performing this mutation, for audit) is taken from the
 * X-Actor-Id header in this scaffold — the real implementation resolves it from
 * the validated JWT/PAT principal once the security-context increment lands, not
 * from a client-supplied header. Left explicit here so this is not mistaken for
 * production-ready auth.
 */
@Path("/api/v1/users")
@Tag(name = "Users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject UserService userService;
    @Inject IdentityManagementGuard guard;

    @POST
    @Operation(summary = "Create a local or federated user")
    public Uni<Response> create(@Valid CreateUserRequest request, @HeaderParam("X-Actor-Id") UUID actorId) {
        return guard.require("identity.user.manage").chain(actor -> userService.createUser(request, actor))
                .map(user -> Response.status(Response.Status.CREATED).entity(user).build());
    }

    @GET
    @Path("/{id}")
    public Uni<UserResponse> getById(@PathParam("id") UUID id) {
        return guard.require("identity.user.read").chain(actor -> userService.getById(id));
    }

    @GET
    public Uni<List<UserResponse>> listAll() {
        return guard.require("identity.user.read").chain(actor -> userService.listAll());
    }

    @PUT
    @Path("/{id}/suspend")
    @Operation(summary = "Suspend a user (blocks login, preserves history)")
    public Uni<UserResponse> suspend(@PathParam("id") UUID id, @HeaderParam("X-Actor-Id") UUID actorId) {
        return guard.require("identity.user.manage").chain(actor -> userService.suspendUser(id, actor));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deprovision a user (SCIM-driven offboarding)")
    public Uni<UserResponse> deprovision(@PathParam("id") UUID id, @HeaderParam("X-Actor-Id") UUID actorId) {
        return guard.require("identity.user.manage").chain(actor -> userService.deprovisionUser(id, actor));
    }
}
