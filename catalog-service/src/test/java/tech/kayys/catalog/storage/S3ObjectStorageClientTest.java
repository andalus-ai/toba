package com.dremioplatform.catalog.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * No live S3/MinIO endpoint is available in this environment (see
 * S3ObjectStorageClient's Javadoc caveat) — this only covers location parsing,
 * which happens before any network call, so it's genuinely testable without one.
 */
class S3ObjectStorageClientTest {

    @Test
    void write_nonS3Location_throwsBeforeAnyNetworkCall() {
        S3ObjectStorageClient client = new S3ObjectStorageClient();
        assertThrows(IllegalArgumentException.class, () -> client.write("/local/path/not-s3", new byte[0]));
    }

    @Test
    void read_malformedS3Uri_throwsBeforeAnyNetworkCall() {
        S3ObjectStorageClient client = new S3ObjectStorageClient();
        assertThrows(IllegalArgumentException.class, () -> client.read("s3://bucket-with-no-key"));
    }
}
