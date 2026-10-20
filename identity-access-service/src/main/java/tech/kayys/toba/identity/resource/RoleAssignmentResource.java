package com.dremioplatform.identity.resource;

import com.dremioplatform.identity.domain.PrincipalType;
import com.dremioplatform.identity.service.RoleAssignmentService;
import com.dremioplatform.security.IdentityManagementGuard;
import com.dremioplatform.identity.service.dto.GrantRoleAssignmentRequest;
import com.dremioplatform.identity.service.dto.RoleAssignmentResponse;
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
import jakarta.ws.rs.core.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/role-assignments")
@Tag(name = "Role Assignments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoleAssignmentResource {

    @Inject RoleAssignmentService roleAssignmentService;
    @Inject IdentityManagementGuard guard;

    @POST
    @Operation(summary = "Grant a role to a principal at a catalog scope")
    public Uni<Response> grant(@Valid GrantRoleAssignmentRequest request, @HeaderParam("X-Actor-Id") UUID actorId) {
        return guard.requireWithinScope("identity.role-assignment.manage", request.scopePath()).chain(actor -> roleAssignmentService.grant(request, actor))
                .map(assignment -> Response.status(Response.Status.CREATED).entity(assignment).build());
    }

    @DELETE
    @Path("/principal/{principalType}/{principalId}/role/{roleId}")
    @Operation(summary = "Revoke a role from a principal")
    public Uni<Response> revoke(@PathParam("principalType") PrincipalType principalType,
                                 @PathParam("principalId") UUID principalId,
                                 @PathParam("roleId") UUID roleId,
                                 @QueryParam("scopePath") String scopePath,
                                 @HeaderParam("X-Actor-Id") UUID actorId) {
        return guard.requireWithinScope("identity.role-assignment.manage", scopePath).chain(actor -> roleAssignmentService.revoke(principalType, principalId, roleId, actor, scopePath))
                .map(v -> Response.noContent().build());
    }

    @GET
    @Path("/principal/{principalType}/{principalId}")
    public Uni<List<RoleAssignmentResponse>> listForPrincipal(@PathParam("principalType") PrincipalType principalType,
                                                                @PathParam("principalId") UUID principalId) {
        return guard.require("identity.role-assignment.manage").chain(actor -> roleAssignmentService.listForPrincipal(principalType, principalId)).map(list -> list.stream().filter(a -> a.scopePath().equals(guard.scope()) || a.scopePath().startsWith(guard.scope() + ".")).toList());
    }

    @GET
    @Path("/principal/{principalType}/{principalId}/effective-permissions")
    @Operation(summary = "Resolve the union of permissions a principal has at a given scope path (debugging/admin UI aid)")
    public Uni<Set<String>> effectivePermissions(@PathParam("principalType") PrincipalType principalType,
                                                   @PathParam("principalId") UUID principalId,
                                                   @QueryParam("scopePath") String scopePath) {
        return guard.requireWithinScope("identity.role-assignment.manage", scopePath).chain(actor -> roleAssignmentService.resolveEffectivePermissions(principalType, principalId, scopePath));
    }
}
