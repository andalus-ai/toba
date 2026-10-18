package com.dremioplatform.adapter;

import com.dremioplatform.adapter.ai.AiFunctionHttpClient;
import com.dremioplatform.adapter.config.AdapterConfig;
import com.dremioplatform.adapter.grpc.AuthCheckGrpcClient;
import com.dremioplatform.adapter.http.MaskingPolicyHttpClient;
import com.dremioplatform.adapter.spi.DremioAccessControlAdapter;
import com.dremioplatform.adapter.spi.DremioAiFunctionRegistryAdapter;
import com.dremioplatform.adapter.spi.DremioMaskingPolicyAdapter;
import java.io.Closeable;

/**
 * The entry point a real Dremio OSS plugin-registration mechanism would call to
 * construct this adapter's pieces (see module README "Wiring this into a real
 * Dremio OSS build"). Owns the lifecycle of the gRPC channel — {@link #close()}
 * must be called on coordinator shutdown, wherever Dremio's plugin lifecycle hook
 * for that is (this scaffold doesn't know that hook's real name/signature; see the
 * same reconciliation caveat as the `spi/` interfaces).
 */
public final class AdapterFactory implements Closeable {

    private final AuthCheckGrpcClient authCheckGrpcClient;
    private final DremioAccessControlAdapter accessControlAdapter;
    private final DremioMaskingPolicyAdapter maskingPolicyAdapter;
    private final DremioAiFunctionRegistryAdapter aiFunctionRegistryAdapter;

    private AdapterFactory(AuthCheckGrpcClient authCheckGrpcClient,
                            DremioAccessControlAdapter accessControlAdapter,
                            DremioMaskingPolicyAdapter maskingPolicyAdapter,
                            DremioAiFunctionRegistryAdapter aiFunctionRegistryAdapter) {
        this.authCheckGrpcClient = authCheckGrpcClient;
        this.accessControlAdapter = accessControlAdapter;
        this.maskingPolicyAdapter = maskingPolicyAdapter;
        this.aiFunctionRegistryAdapter = aiFunctionRegistryAdapter;
    }

    public static AdapterFactory create() {
        return create(AdapterConfig.loadDefault());
    }

    public static AdapterFactory create(AdapterConfig config) {
        AuthCheckGrpcClient grpcClient = new AuthCheckGrpcClient(config);
        MaskingPolicyHttpClient maskingHttpClient = new MaskingPolicyHttpClient(config);
        AiFunctionHttpClient aiFunctionHttpClient = new AiFunctionHttpClient(config);
        return new AdapterFactory(
                grpcClient,
                new DremioAccessControlAdapter(grpcClient),
                new DremioMaskingPolicyAdapter(maskingHttpClient),
                new DremioAiFunctionRegistryAdapter(aiFunctionHttpClient));
    }

    public DremioAccessControlAdapter accessControlAdapter() {
        return accessControlAdapter;
    }

    public DremioMaskingPolicyAdapter maskingPolicyAdapter() {
        return maskingPolicyAdapter;
    }

    /** Register with Dremio's {@code PluggableFunctionRegistry} classpath-scan
     *  mechanism (RECONCILIATION_NOTES.md Finding 3) — read that finding's
     *  scale/blocking-I/O warning before wiring this into a real query path. */
    public DremioAiFunctionRegistryAdapter aiFunctionRegistryAdapter() {
        return aiFunctionRegistryAdapter;
    }

    @Override
    public void close() {
        authCheckGrpcClient.close();
    }
}
