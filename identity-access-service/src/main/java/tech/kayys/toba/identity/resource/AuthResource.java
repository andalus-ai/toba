package com.dremioplatform.identity.resource;

import com.dremioplatform.identity.service.TokenService;
import com.dremioplatform.identity.service.dto.AccessTokenResponse;
import com.dremioplatform.identity.service.dto.LoginRequest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Local-credential login only. Federated login (OIDC redirect/callback, LDAP bind)
 * is a separate, not-yet-built flow — see module README "Known gaps".
 */
@Path("/api/v1/auth")
@Tag(name = "Auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject TokenService tokenService;

    @POST
    @Path("/login")
    @Operation(summary = "Exchange local username/password for a short-lived access JWT")
    public Uni<AccessTokenResponse> login(@Valid LoginRequest request) {
        return tokenService.loginLocal(request);
    }
}
