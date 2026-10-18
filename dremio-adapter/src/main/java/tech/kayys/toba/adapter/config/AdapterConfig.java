package com.dremioplatform.adapter.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Deliberately not using MicroProfile Config / Quarkus config — this jar runs
 * inside Dremio's JVM, which has its own config system (Dremio reads
 * dremio.conf/HOCON). In a real integration, values here are populated from
 * whatever config surface Dremio's plugin-registration mechanism exposes to a
 * registered {@code StoragePlugin}/{@code AccessControlChecker} implementation
 * (constructor injection of a config object, typically) rather than loaded from a
 * standalone properties file the way this scaffold does for local testing.
 */
public final class AdapterConfig {

    private final Properties properties;

    private AdapterConfig(Properties properties) {
        this.properties = properties;
    }

    public static AdapterConfig loadDefault() {
        Properties props = new Properties();
        try (InputStream in = AdapterConfig.class.getResourceAsStream("/dremio-adapter.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load dremio-adapter.properties", e);
        }
        return new AdapterConfig(props);
    }

    public String identityGrpcHost() {
        return systemOverride("dremio.adapter.identity.grpc.host", "identity.grpc.host", "localhost");
    }

    public int identityGrpcPort() {
        return Integer.parseInt(systemOverride("dremio.adapter.identity.grpc.port", "identity.grpc.port", "9090"));
    }

    public String governanceBaseUrl() {
        return systemOverride("dremio.adapter.governance.base-url", "governance.base-url", "http://localhost:8081");
    }

    public long authCheckTimeoutMillis() {
        return Long.parseLong(systemOverride("dremio.adapter.auth-check.timeout-ms", "auth-check.timeout-ms", "500"));
    }

    public long maskingPolicyCacheTtlMillis() {
        return Long.parseLong(systemOverride("dremio.adapter.masking-policy.cache-ttl-ms", "masking-policy.cache-ttl-ms", "30000"));
    }

    public String semanticAiBaseUrl() {
        return systemOverride("dremio.adapter.semantic-ai.base-url", "semantic-ai.base-url", "http://localhost:8083");
    }

    public long aiFunctionTimeoutMillis() {
        return Long.parseLong(systemOverride("dremio.adapter.ai-function.timeout-ms", "ai-function.timeout-ms", "10000"));
    }

    private String systemOverride(String systemPropertyKey, String propertiesKey, String fallback) {
        String systemValue = System.getProperty(systemPropertyKey);
        if (systemValue != null) {
            return systemValue;
        }
        return properties.getProperty(propertiesKey, fallback);
    }
}
