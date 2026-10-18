package com.dremioplatform.catalog.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/** Plain unit test — mocks both delegate clients directly, since
 *  RoutingObjectStorageClient's fields are package-visible. Confirms the routing
 *  decision itself, not either backend's actual I/O (covered elsewhere). */
class RoutingObjectStorageClientTest {

    @Test
    void backendLocal_routesToLocalClientOnly() {
        LocalFilesystemObjectStorageClient local = mock(LocalFilesystemObjectStorageClient.class);
        S3ObjectStorageClient s3 = mock(S3ObjectStorageClient.class);
        when(local.read("some-location")).thenReturn(new byte[] {1, 2, 3});

        RoutingObjectStorageClient router = new RoutingObjectStorageClient();
        router.localClient = local;
        router.s3Client = s3;
        router.backend = "local";

        assertArrayEquals(new byte[] {1, 2, 3}, router.read("some-location"));
        verifyNoInteractions(s3);
    }

    @Test
    void backendS3_routesToS3ClientOnly() {
        LocalFilesystemObjectStorageClient local = mock(LocalFilesystemObjectStorageClient.class);
        S3ObjectStorageClient s3 = mock(S3ObjectStorageClient.class);
        when(s3.read("s3://bucket/key")).thenReturn(new byte[] {4, 5, 6});

        RoutingObjectStorageClient router = new RoutingObjectStorageClient();
        router.localClient = local;
        router.s3Client = s3;
        router.backend = "s3";

        assertArrayEquals(new byte[] {4, 5, 6}, router.read("s3://bucket/key"));
        verifyNoInteractions(local);
    }

    @Test
    void write_routesToSelectedBackend() {
        LocalFilesystemObjectStorageClient local = mock(LocalFilesystemObjectStorageClient.class);
        S3ObjectStorageClient s3 = mock(S3ObjectStorageClient.class);

        RoutingObjectStorageClient router = new RoutingObjectStorageClient();
        router.localClient = local;
        router.s3Client = s3;
        router.backend = "local";

        byte[] content = "hello".getBytes();
        router.write("loc", content);

        verify(local).write("loc", content);
        verifyNoInteractions(s3);
    }
}
