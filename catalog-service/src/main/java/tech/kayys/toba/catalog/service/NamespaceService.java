package com.dremioplatform.catalog.service;

import com.dremioplatform.catalog.domain.CatalogNamespace;
import com.dremioplatform.catalog.repository.CatalogNamespaceRepository;
import com.dremioplatform.catalog.repository.CatalogTableRepository;
import com.dremioplatform.catalog.service.dto.CreateNamespaceRequest;
import com.dremioplatform.catalog.service.dto.ListNamespacesResponse;
import com.dremioplatform.catalog.service.dto.NamespaceResponse;
import com.dremioplatform.catalog.util.NamespaceCodec;
import com.dremioplatform.common.error.ConflictException;
import com.dremioplatform.common.error.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class NamespaceService {

    @Inject CatalogNamespaceRepository namespaceRepository;
    @Inject CatalogTableRepository tableRepository;
    @Inject ObjectMapper objectMapper;

    @WithTransaction
    public Uni<NamespaceResponse> create(CreateNamespaceRequest request) {
        String storagePath = NamespaceCodec.toStoragePath(request.namespace());
        return namespaceRepository.existsByLevelsPath(storagePath)
                .chain(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(new ConflictException(
                                "CATALOG-409-NAMESPACE-EXISTS", "Namespace already exists: " + storagePath));
                    }
                    CatalogNamespace ns = new CatalogNamespace();
                    ns.id = UUID.randomUUID();
                    ns.levelsPath = storagePath;
                    ns.propertiesJson = toJson(request.properties() == null ? Map.of() : request.properties());
                    return namespaceRepository.persist(ns)
                            .map(saved -> new NamespaceResponse(request.namespace(), request.properties()));
                });
    }

    public Uni<NamespaceResponse> get(String wireEncodedNamespace) {
        String storagePath = NamespaceCodec.wireFormatToStoragePath(wireEncodedNamespace);
        return namespaceRepository.findByLevelsPath(storagePath)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "CATALOG-404-NAMESPACE-NOT-FOUND", "Namespace", storagePath))
                .map(ns -> new NamespaceResponse(NamespaceCodec.fromStoragePath(ns.levelsPath), fromJson(ns.propertiesJson)));
    }

    public Uni<ListNamespacesResponse> listChildren(String wireEncodedParentOrNull) {
        String parentStoragePath = wireEncodedParentOrNull == null || wireEncodedParentOrNull.isBlank()
                ? null : NamespaceCodec.wireFormatToStoragePath(wireEncodedParentOrNull);
        return namespaceRepository.findChildrenOf(parentStoragePath)
                .map(list -> new ListNamespacesResponse(
                        list.stream().map(ns -> NamespaceCodec.fromStoragePath(ns.levelsPath)).toList()));
    }

    @WithTransaction
    public Uni<Void> delete(String wireEncodedNamespace) {
        String storagePath = NamespaceCodec.wireFormatToStoragePath(wireEncodedNamespace);
        return tableRepository.listInNamespace(storagePath)
                .chain(tables -> {
                    if (!tables.isEmpty()) {
                        return Uni.createFrom().failure(new ConflictException(
                                "CATALOG-409-NAMESPACE-NOT-EMPTY",
                                "Namespace has " + tables.size() + " table(s); drop them first"));
                    }
                    return namespaceRepository.findByLevelsPath(storagePath)
                            .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                                    "CATALOG-404-NAMESPACE-NOT-FOUND", "Namespace", storagePath))
                            .chain(namespaceRepository::delete);
                });
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize namespace properties", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
