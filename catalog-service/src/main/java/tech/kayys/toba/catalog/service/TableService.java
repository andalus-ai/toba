package com.dremioplatform.catalog.service;

import com.dremioplatform.catalog.domain.CatalogTable;
import com.dremioplatform.catalog.event.CatalogEventPublisher;
import com.dremioplatform.catalog.event.CatalogTableEvent;
import com.dremioplatform.catalog.repository.CatalogNamespaceRepository;
import com.dremioplatform.catalog.repository.CatalogTableRepository;
import com.dremioplatform.catalog.service.dto.CreateTableRequest;
import com.dremioplatform.catalog.service.dto.ListTablesResponse;
import com.dremioplatform.catalog.service.dto.LoadTableResponse;
import com.dremioplatform.catalog.storage.ObjectStorageClient;
import com.dremioplatform.catalog.util.NamespaceCodec;
import com.dremioplatform.common.error.ConflictException;
import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.common.error.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SchemaParser;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.TableMetadataParser;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Table CRUD backed by a REAL Iceberg {@link TableMetadata} — verified against
 * Apache Iceberg 1.11.0's actual source (a shallow, blob-filtered clone of
 * github.com/apache/iceberg, same research technique used for the dremio-oss
 * reconciliation — see {@code dremio-adapter/RECONCILIATION_NOTES.md} for that
 * precedent) before writing any code against it. This replaces an earlier version
 * of this class that only synthesized a placeholder metadata-location path and
 * never wrote or read an actual file.
 *
 * <p>What's real now:
 * <ul>
 *   <li>{@link #create} parses the caller's schema JSON with the real
 *       {@code SchemaParser.fromJson}, builds a real {@code TableMetadata} via
 *       {@code TableMetadata.newTableMetadata(Schema, PartitionSpec, String, Map)},
 *       serializes it with the real {@code TableMetadataParser.toJson}, and writes
 *       those bytes through {@link ObjectStorageClient} to the table's metadata
 *       location.
 *   <li>{@link #load} reads those bytes back and re-parses them with
 *       {@code TableMetadataParser.fromJson(String)} — a genuine round trip, not a
 *       cached DTO.
 * </ul>
 *
 * <p>What's still simplified — see module README "Known gaps" for the full list:
 * <ul>
 *   <li><b>Partition spec is always {@code PartitionSpec.unpartitioned()}</b> —
 *       {@link CreateTableRequest} has no partition-spec field yet.
 *   <li><b>Sort order is always unsorted</b> (Iceberg's default when omitted from
 *       {@code newTableMetadata}'s 4-arg overload) — same reason.
 *   <li><b>No commit-transaction endpoint</b> — nothing appends new snapshots
 *       after creation; {@code TableSnapshot} the domain entity exists and is
 *       migrated, but this class never writes to it.
 *   <li><b>{@link ObjectStorageClient} has two implementations now</b> — local
 *       disk (default) and real S3/S3-compatible (opt-in via
 *       {@code catalog.storage.backend=s3}) — see {@code storage/RoutingObjectStorageClient}'s
 *       Javadoc. Both are genuinely blocking (disk or network I/O), so every call
 *       through the client here runs via {@code Uni.createFrom().item(...).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())}
 *       rather than inline on the reactive event loop — a correctness fix made in
 *       the same pass that added the S3 backend, since the original local-disk-only
 *       version had this same blocking-I/O-on-the-event-loop issue and simply
 *       hadn't been caught yet.
 * </ul>
 */
@ApplicationScoped
public class TableService {

    @Inject CatalogTableRepository tableRepository;
    @Inject CatalogNamespaceRepository namespaceRepository;
    @Inject CatalogEventPublisher eventPublisher;
    @Inject ObjectMapper objectMapper;
    @Inject ObjectStorageClient objectStorageClient;

    @ConfigProperty(name = "catalog.warehouse.root")
    String warehouseRoot;

    @WithTransaction
    public Uni<LoadTableResponse> create(String wireEncodedNamespace, CreateTableRequest request, UUID actorPrincipalId) {
        String namespacePath = NamespaceCodec.wireFormatToStoragePath(wireEncodedNamespace);

        Schema schema;
        try {
            schema = SchemaParser.fromJson(request.schemaJson());
        } catch (RuntimeException e) {
            return Uni.createFrom().failure(new ValidationException(
                    "CATALOG-400-INVALID-SCHEMA", "schemaJson is not a valid Iceberg schema: " + e.getMessage()));
        }

        return namespaceRepository.existsByLevelsPath(namespacePath)
                .chain(namespaceExists -> {
                    if (!namespaceExists) {
                        return Uni.createFrom().failure(new EntityNotFoundException(
                                "CATALOG-404-NAMESPACE-NOT-FOUND", "Namespace", namespacePath));
                    }
                    return tableRepository.exists(namespacePath, request.name());
                })
                .chain(tableExists -> {
                    if (Boolean.TRUE.equals(tableExists)) {
                        return Uni.createFrom().failure(new ConflictException(
                                "CATALOG-409-TABLE-EXISTS",
                                "Table already exists: " + namespacePath + "." + request.name()));
                    }

                    CatalogTable table = new CatalogTable();
                    table.id = UUID.randomUUID();
                    table.namespacePath = namespacePath;
                    table.tableName = request.name();
                    String tableLocation = warehouseRoot + "/" + namespacePath.replace('.', '/') + "/" + request.name();
                    table.metadataLocation = tableLocation + "/metadata/v1-" + table.id + ".metadata.json";

                    Map<String, String> properties = request.properties() == null ? Map.of() : request.properties();
                    table.propertiesJson = toJson(properties);

                    TableMetadata metadata = TableMetadata.newTableMetadata(
                            schema, PartitionSpec.unpartitioned(), tableLocation, properties);
                    String metadataJson = TableMetadataParser.toJson(metadata);

                    // Both LocalFilesystemObjectStorageClient (disk I/O) and
                    // S3ObjectStorageClient (network I/O) are genuinely blocking —
                    // offload off the reactive event loop rather than stalling it.
                    return Uni.createFrom().item(() -> {
                                objectStorageClient.writeString(table.metadataLocation, metadataJson);
                                return null;
                            })
                            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                            .chain(ignored -> tableRepository.persist(table)
                                    .call(t -> eventPublisher.publish(CatalogTableEvent.of(
                                            "TABLE_CREATED", t.namespacePath, t.tableName, actorPrincipalId)))
                                    .map(t -> new LoadTableResponse(t.metadataLocation, t.currentSnapshotId, properties, metadataJson)));
                });
    }

    public Uni<LoadTableResponse> load(String wireEncodedNamespace, String tableName) {
        String namespacePath = NamespaceCodec.wireFormatToStoragePath(wireEncodedNamespace);
        return tableRepository.findByNamespaceAndName(namespacePath, tableName)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "CATALOG-404-TABLE-NOT-FOUND", "Table", namespacePath + "." + tableName))
                .chain(t -> Uni.createFrom().item(() -> {
                            String metadataJson = objectStorageClient.readString(t.metadataLocation);
                            // Round-trip through the real parser — not strictly needed to return
                            // the JSON (already have it as a string), but validates that what's
                            // stored is still parseable Iceberg TableMetadata, catching storage
                            // corruption or an incompatible manual edit rather than silently
                            // returning bytes nobody's confirmed are still valid.
                            TableMetadataParser.fromJson(metadataJson);
                            return new LoadTableResponse(t.metadataLocation, t.currentSnapshotId, fromJson(t.propertiesJson), metadataJson);
                        })
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()));
    }

    public Uni<ListTablesResponse> list(String wireEncodedNamespace) {
        String namespacePath = NamespaceCodec.wireFormatToStoragePath(wireEncodedNamespace);
        return tableRepository.listInNamespace(namespacePath)
                .map(tables -> new ListTablesResponse(
                        tables.stream()
                                .map(t -> new ListTablesResponse.TableIdentifier(
                                        NamespaceCodec.fromStoragePath(t.namespacePath), t.tableName))
                                .toList()));
    }

    @WithTransaction
    public Uni<Void> drop(String wireEncodedNamespace, String tableName, UUID actorPrincipalId) {
        String namespacePath = NamespaceCodec.wireFormatToStoragePath(wireEncodedNamespace);
        return tableRepository.findByNamespaceAndName(namespacePath, tableName)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "CATALOG-404-TABLE-NOT-FOUND", "Table", namespacePath + "." + tableName))
                .chain(table -> tableRepository.delete(table)
                        .chain(() -> eventPublisher.publish(CatalogTableEvent.of(
                                "TABLE_DROPPED", namespacePath, tableName, actorPrincipalId))));
        // NOTE: does not delete the underlying metadata JSON from object storage —
        // Iceberg's own convention is that dropping a table via a catalog doesn't
        // necessarily purge its files (a "purge" drop is a distinct, more dangerous
        // operation) — this catalog only removes the pointer, matching that convention
        // rather than accidentally being more destructive than intended.
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize table properties", e);
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
