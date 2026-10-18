package com.dremioplatform.catalog.storage;

import java.nio.charset.StandardCharsets;

/**
 * Where table metadata JSON (and, eventually, data files) actually live. Kept as
 * a small interface — not tied to any specific object-storage SDK — so the write/
 * read path used by {@link com.dremioplatform.catalog.service.TableService} stays
 * the same regardless of which implementation is active.
 *
 * Two implementations exist: {@link LocalFilesystemObjectStorageClient} (default,
 * dev-safe, writes under a configured local directory) and
 * {@link S3ObjectStorageClient} (real S3/S3-compatible, opt-in via
 * {@code catalog.storage.backend=s3}). {@link RoutingObjectStorageClient} is the
 * only bean that actually implements this interface — see its Javadoc for why
 * (CDI ambiguity avoidance, same pattern as `semantic-ai-service`'s
 * `RoutingLlmClient`). This is the same "real logic, swappable backend, safe local
 * default" shape as {@code K8sScalingClient} (workload-orchestrator-service) and
 * {@code MockLlmClient}/`RoutingLlmClient` (semantic-ai-service) — consistent with
 * the rest of this platform rather than a one-off pattern for this service.
 */
public interface ObjectStorageClient {

    void write(String location, byte[] content);

    byte[] read(String location);

    boolean exists(String location);

    default void writeString(String location, String content) {
        write(location, content.getBytes(StandardCharsets.UTF_8));
    }

    default String readString(String location) {
        return new String(read(location), StandardCharsets.UTF_8);
    }

    /** Thrown when {@link #read} is given a location that doesn't exist —
     *  distinguishes "not found" from other I/O failures for callers that want
     *  to handle it differently (e.g. treat as 404 vs. 500). */
    class ObjectNotFoundException extends RuntimeException {
        public ObjectNotFoundException(String location, Throwable cause) {
            super("Object not found: " + location, cause);
        }
    }
}
