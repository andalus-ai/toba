package com.dremioplatform.catalog.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * The only {@link ObjectStorageClient} bean in this service — routes to
 * {@link LocalFilesystemObjectStorageClient} (default, `catalog.storage.backend=local`)
 * or {@link S3ObjectStorageClient} (`catalog.storage.backend=s3`) based on config.
 * Same "plain delegate beans + one routing bean implementing the shared interface"
 * shape as `semantic-ai-service`'s `RoutingLlmClient` — kept consistent across the
 * repo rather than inventing a new pattern for this one case.
 */
@ApplicationScoped
public class RoutingObjectStorageClient implements ObjectStorageClient {

    @Inject LocalFilesystemObjectStorageClient localClient;
    @Inject S3ObjectStorageClient s3Client;

    @ConfigProperty(name = "catalog.storage.backend")
    String backend;

    @Override
    public void write(String location, byte[] content) {
        if (isS3()) {
            s3Client.write(location, content);
        } else {
            localClient.write(location, content);
        }
    }

    @Override
    public byte[] read(String location) {
        return isS3() ? s3Client.read(location) : localClient.read(location);
    }

    @Override
    public boolean exists(String location) {
        return isS3() ? s3Client.exists(location) : localClient.exists(location);
    }

    private boolean isS3() {
        return "s3".equalsIgnoreCase(backend);
    }
}
