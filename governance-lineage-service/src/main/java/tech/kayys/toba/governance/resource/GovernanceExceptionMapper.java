package com.dremioplatform.governance.resource;

import com.dremioplatform.common.error.PlatformException;
import com.dremioplatform.common.error.ProblemDetail;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GovernanceExceptionMapper implements ExceptionMapper<PlatformException> {

    @Context UriInfo uriInfo;

    @Override
    public Response toResponse(PlatformException exception) {
        String instance = uriInfo != null ? uriInfo.getPath() : "unknown";
        return Response.status(exception.httpStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(ProblemDetail.of(exception, instance))
                .build();
    }
}
