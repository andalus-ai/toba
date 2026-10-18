package com.dremioplatform.catalog.storage;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Writes/reads table metadata as real files under a local directory — the
 * always-available fallback (see {@link RoutingObjectStorageClient}, which
 * decides at runtime whether to delegate here or to {@link S3ObjectStorageClient}
 * based on {@code catalog.storage.backend}). `location` strings still use the
 * {@code s3://bucket/...} shape {@code catalog.warehouse.root} is configured
 * with (matching what a real deployment's locations would look like), but the
 * scheme/bucket segment is stripped and the remainder resolved under
 * `catalog.storage.local-root` — this keeps
 * {@link com.dremioplatform.catalog.service.TableService}'s location-building
 * logic identical regardless of which storage backend is actually active.
 *
 * Deliberately does NOT implement {@link ObjectStorageClient} directly — only
 * {@link RoutingObjectStorageClient} does, so there's no CDI ambiguity between
 * this class and {@link S3ObjectStorageClient} for anything injecting
 * {@code ObjectStorageClient} (same pattern as `semantic-ai-service`'s
 * `MockLlmClient`/`AnthropicChatClient` vs. `RoutingLlmClient`).
 */
@ApplicationScoped
public class LocalFilesystemObjectStorageClient {

    @ConfigProperty(name = "catalog.storage.local-root")
    String localRoot;

    public void write(String location, byte[] content) {
        Path path = resolve(location);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + location, e);
        }
    }

    public byte[] read(String location) {
        Path path = resolve(location);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + location, e);
        }
    }

    public boolean exists(String location) {
        return Files.exists(resolve(location));
    }

    /** Strips a {@code scheme://bucket/} prefix (if present) and resolves the rest
     *  under the configured local root, so {@code s3://my-bucket/a/b.json} and
     *  {@code a/b.json} both land at {@code <local-root>/a/b.json}. */
    private Path resolve(String location) {
        String relative = location;
        int schemeIdx = relative.indexOf("://");
        if (schemeIdx >= 0) {
            String afterScheme = relative.substring(schemeIdx + 3);
            int firstSlash = afterScheme.indexOf('/');
            relative = firstSlash >= 0 ? afterScheme.substring(firstSlash + 1) : "";
        }
        return Paths.get(localRoot, relative);
    }
}
