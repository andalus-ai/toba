package com.dremioplatform.catalog.resource;

import com.dremioplatform.catalog.service.TableService;
import com.dremioplatform.catalog.security.CatalogAccessGuard;
import com.dremioplatform.catalog.service.dto.CreateTableRequest;
import com.dremioplatform.catalog.service.dto.ListTablesResponse;
import com.dremioplatform.catalog.service.dto.LoadTableResponse;
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
import jakarta.ws.rs.core.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Implements the Iceberg REST Catalog spec's table endpoints under a namespace.
 * See {@link TableService}'s Javadoc for exactly which parts of the spec (schema
 * validation, real TableMetadata JSON, commit-transaction/update) are not yet
 * spec-complete in this increment.
 */
@Path("/v1/namespaces/{namespace}/tables")
@Tag(name = "Iceberg REST Catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TableResource {

    @Inject TableService tableService;
    @Inject CatalogAccessGuard access;
    @Context SecurityContext securityContext;

    @GET
    @Operation(summary = "List tables in a namespace")
    public Uni<ListTablesResponse> list(@PathParam("namespace") String namespace) {
        access.require(securityContext, "catalog.table.list", namespace);
        return tableService.list(namespace);
    }

    @POST
    @Operation(summary = "Create a table in a namespace")
    public Uni<Response> create(@PathParam("namespace") String namespace,
                                 @Valid CreateTableRequest request,
                                 @HeaderParam("X-Actor-Id") UUID actorId) {
        access.require(securityContext, "catalog.table.create", namespace);
        return tableService.create(namespace, request, actorId)
                .map(table -> Response.status(Response.Status.OK).entity(table).build());
    }

    @GET
    @Path("/{table}")
    @Operation(summary = "Load a table's current metadata pointer")
    public Uni<LoadTableResponse> load(@PathParam("namespace") String namespace, @PathParam("table") String table) {
        access.require(securityContext, "catalog.table.read", namespace + "/" + table);
        return tableService.load(namespace, table);
    }

    @DELETE
    @Path("/{table}")
    @Operation(summary = "Drop a table")
    public Uni<Response> drop(@PathParam("namespace") String namespace,
                               @PathParam("table") String table,
                               @HeaderParam("X-Actor-Id") UUID actorId) {
        access.require(securityContext, "catalog.table.delete", namespace + "/" + table);
        return tableService.drop(namespace, table, actorId).map(v -> Response.noContent().build());
    }
}
