package com.dremioplatform.catalog.resource;

import com.dremioplatform.catalog.service.NamespaceService;
import com.dremioplatform.catalog.security.CatalogAccessGuard;
import com.dremioplatform.catalog.service.dto.CreateNamespaceRequest;
import com.dremioplatform.catalog.service.dto.ListNamespacesResponse;
import com.dremioplatform.catalog.service.dto.NamespaceResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Implements the Iceberg REST Catalog spec's namespace endpoints
 * (https://iceberg.apache.org/spec/#rest-catalog). {namespace} path segments are the
 * spec's unit-separator-encoded multi-level format — see
 * {@link com.dremioplatform.catalog.util.NamespaceCodec}.
 */
@Path("/v1/namespaces")
@Tag(name = "Iceberg REST Catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NamespaceResource {

    @Inject NamespaceService namespaceService;
    @Inject CatalogAccessGuard access;
    @Context SecurityContext securityContext;

    @GET
    @Operation(summary = "List namespaces, optionally under a parent")
    public Uni<ListNamespacesResponse> list(@QueryParam("parent") String parent) {
        access.require(securityContext, "catalog.namespace.list", parent == null ? "root" : parent);
        return namespaceService.listChildren(parent);
    }

    @POST
    @Operation(summary = "Create a namespace")
    public Uni<Response> create(@Valid CreateNamespaceRequest request) {
        access.require(securityContext, "catalog.namespace.create", "namespaces");
        return namespaceService.create(request)
                .map(ns -> Response.status(Response.Status.OK).entity(ns).build());
    }

    @GET
    @Path("/{namespace}")
    @Operation(summary = "Load a namespace's properties")
    public Uni<NamespaceResponse> get(@PathParam("namespace") String namespace) {
        access.require(securityContext, "catalog.namespace.read", namespace);
        return namespaceService.get(namespace);
    }

    @HEAD
    @Path("/{namespace}")
    @Operation(summary = "Check whether a namespace exists (spec requires a HEAD variant)")
    public Uni<Response> exists(@PathParam("namespace") String namespace) {
        access.require(securityContext, "catalog.namespace.read", namespace);
        return namespaceService.get(namespace)
                .map(ns -> Response.ok().build())
                .onFailure().recoverWithItem(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{namespace}")
    @Operation(summary = "Drop an empty namespace")
    public Uni<Response> delete(@PathParam("namespace") String namespace) {
        access.require(securityContext, "catalog.namespace.delete", namespace);
        return namespaceService.delete(namespace).map(v -> Response.noContent().build());
    }
}
