package com.dremioplatform.identity.resource;

import com.dremioplatform.identity.service.ServiceAccountService;
import com.dremioplatform.identity.service.dto.CreateServiceAccountRequest;
import com.dremioplatform.identity.service.dto.ServiceAccountResponse;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/service-accounts")
@Tag(name = "Service Accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceAccountResource {

    @Inject ServiceAccountService serviceAccountService;

    @POST
    public Uni<Response> create(@Valid CreateServiceAccountRequest request) {
        return serviceAccountService.create(request)
                .map(sa -> Response.status(Response.Status.CREATED).entity(sa).build());
    }

    @PUT
    @Path("/{id}/disable")
    public Uni<ServiceAccountResponse> disable(@PathParam("id") UUID id) {
        return serviceAccountService.disable(id);
    }

    @GET
    public Uni<List<ServiceAccountResponse>> listAll() {
        return serviceAccountService.listAll();
    }
}
