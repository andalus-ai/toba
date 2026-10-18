package com.dremioplatform.catalog.storage;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Real S3-compatible object storage, via the plain AWS SDK v2 (not a Quarkus
 * extension — see the dependency comment in pom.xml for why). Works against real
 * AWS S3 or any S3-compatible endpoint (MinIO, etc.) via
 * `catalog.storage.s3.endpoint-override`. Uses the synchronous `S3Client` with the
 * `url-connection-client` HTTP client — the AWS SDK's own documented
 * recommendation for a lightweight blocking client (see pom.xml comment) — since
 * {@link RoutingObjectStorageClient}'s interface is itself synchronous;
 * {@link com.dremioplatform.catalog.service.TableService} is responsible for
 * offloading these blocking calls off the reactive event loop (see that class's
 * use of `Uni...runSubscriptionOn`), not this client.
 *
 * `location` strings are parsed as `s3://bucket/key` — this is the one
 * implementation where that scheme is actually meaningful, unlike
 * {@link LocalFilesystemObjectStorageClient} which strips it.
 *
 * Deliberately does NOT implement {@link ObjectStorageClient} directly — see that
 * class's Javadoc and {@link LocalFilesystemObjectStorageClient}'s for why (CDI
 * ambiguity avoidance; only {@link RoutingObjectStorageClient} implements it).
 *
 * NOT independently verified end-to-end against a real S3 bucket or MinIO in this
 * environment (no S3-compatible endpoint reachable here) — the AWS SDK v2 API
 * calls used (`S3Client.putObject`/`getObject`/`headObject`) were checked against
 * the SDK's own current Javadoc/README before writing this, following the same
 * verify-before-build discipline as the Iceberg and dremio-oss work, but "checked
 * the API shape" is a weaker claim than "tested against a live bucket" — flagged
 * explicitly rather than implied.
 */
@ApplicationScoped
public class S3ObjectStorageClient {

    private static final Pattern S3_URI = Pattern.compile("^s3://([^/]+)/(.+)$");

    @ConfigProperty(name = "catalog.storage.s3.region")
    String region;

    @ConfigProperty(name = "catalog.storage.s3.endpoint-override")
    java.util.Optional<String> endpointOverride;

    @ConfigProperty(name = "catalog.storage.s3.access-key")
    java.util.Optional<String> accessKey;

    @ConfigProperty(name = "catalog.storage.s3.secret-key")
    java.util.Optional<String> secretKey;

    @ConfigProperty(name = "catalog.storage.s3.path-style-access")
    boolean pathStyleAccess;

    private volatile S3Client client;

    public void write(String location, byte[] content) {
        BucketKey bk = parse(location);
        s3Client().putObject(
                PutObjectRequest.builder().bucket(bk.bucket()).key(bk.key()).build(),
                RequestBody.fromBytes(content));
    }

    public byte[] read(String location) {
        BucketKey bk = parse(location);
        try {
            return s3Client().getObject(
                    GetObjectRequest.builder().bucket(bk.bucket()).key(bk.key()).build()).readAllBytes();
        } catch (NoSuchKeyException e) {
            throw new ObjectStorageClient.ObjectNotFoundException(location, e);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Failed to read S3 object body for " + location, e);
        }
    }

    public boolean exists(String location) {
        BucketKey bk = parse(location);
        try {
            s3Client().headObject(HeadObjectRequest.builder().bucket(bk.bucket()).key(bk.key()).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    /** Lazily built, not a CDI producer — endpoint/credentials config can change
     *  between test runs (see S3ObjectStorageClientTest), and this avoids paying
     *  for a client construction on every app that never actually uses S3. */
    private S3Client s3Client() {
        S3Client local = client;
        if (local == null) {
            synchronized (this) {
                if (client == null) {
                    S3Client.Builder builder = S3Client.builder()
                            .region(Region.of(region))
                            .forcePathStyle(pathStyleAccess);
                    endpointOverride.ifPresent(url -> builder.endpointOverride(URI.create(url)));
                    builder.credentialsProvider(credentialsProvider());
                    client = builder.build();
                }
                local = client;
            }
        }
        return local;
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (accessKey.isPresent() && secretKey.isPresent()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.get(), secretKey.get()));
        }
        // Falls back to the SDK's normal credential chain (env vars, ~/.aws/credentials,
        // instance profile, etc.) — appropriate for real AWS deployments; explicit
        // access/secret key config above is the common path for MinIO/local S3-compatible testing.
        return DefaultCredentialsProvider.create();
    }

    private static BucketKey parse(String location) {
        Matcher matcher = S3_URI.matcher(location);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a valid s3:// location: " + location);
        }
        return new BucketKey(matcher.group(1), matcher.group(2));
    }

    private record BucketKey(String bucket, String key) {
    }
}
