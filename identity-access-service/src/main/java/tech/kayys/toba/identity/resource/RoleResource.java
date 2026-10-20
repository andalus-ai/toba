package com.dremioplatform.identity.resource;

import com.dremioplatform.identity.service.RoleService;
import com.dremioplatform.security.IdentityManagementGuard;
import com.dremioplatform.identity.service.dto.CreateRoleRequest;
import com.dremioplatform.identity.service.dto.RoleResponse;
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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/roles")
@Tag(name = "Roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoleResource {

    @Inject RoleService roleService;
    @Inject IdentityManagementGuard guard;

    @POST
    public Uni<Response> create(@Valid CreateRoleRequest request) {
        return guard.require("identity.role.manage").chain(actor -> roleService.createRole(request))
                .map(role -> Response.status(Response.Status.CREATED).entity(role).build());
    }

    @GET
    @Path("/{id}")
    public Uni<RoleResponse> getById(@PathParam("id") UUID id) {
        return guard.require("identity.role.read").chain(actor -> roleService.getById(id));
    }

    @GET
    public Uni<List<RoleResponse>> listAll() {
        return guard.require("identity.role.read").chain(actor -> roleService.listAll());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> delete(@PathParam("id") UUID id) {
        return guard.require("identity.role.manage").chain(actor -> roleService.deleteRole(id)).map(v -> Response.noContent().build());
    }
}
