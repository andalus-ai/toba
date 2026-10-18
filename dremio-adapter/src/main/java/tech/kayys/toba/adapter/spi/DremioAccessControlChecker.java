package com.dremioplatform.adapter.spi;

/**
 * VERIFIED against real dremio-oss source (see RECONCILIATION_NOTES.md at this
 * module's root for how — a shallow, blob-filtered clone of
 * github.com/dremio/dremio-oss inspected directly). This replaces an earlier,
 * unverified guess at this interface's shape.
 *
 * <p><b>The real hook is NOT a standalone, globally-registered "AccessControlChecker"
 * interface.</b> It's a single method on {@code com.dremio.exec.store.StoragePlugin}
 * itself (file: {@code sabot/kernel/src/main/java/com/dremio/exec/store/StoragePlugin.java}):
 *
 * <pre>{@code
 * boolean hasAccessPermission(String user, NamespaceKey key, DatasetConfig datasetConfig);
 * }</pre>
 *
 * where {@code NamespaceKey} is {@code com.dremio.service.namespace.NamespaceKey}
 * (module {@code services/namespace}) and {@code DatasetConfig} is
 * {@code com.dremio.service.namespace.dataset.proto.DatasetConfig} (a protobuf type).
 *
 * <p><b>Three operational facts that change how this integrates</b>, all from the
 * real method's Javadoc and its caller ({@code com.dremio.exec.catalog.PermissionCheckCache}):
 * <ol>
 *   <li><b>Per-source, not global.</b> Every {@code StoragePlugin} implementation
 *       (one per registered Dremio source) has its own {@code hasAccessPermission}.
 *       There's no single place to register one checker for the whole cluster —
 *       this adapter's access-control piece must be wired per source, likely by
 *       providing a custom {@code StoragePlugin} (or a decorator around an existing
 *       one, the same pattern dremio-oss's own internal
 *       {@code SourceAccessChecker implements Catalog} decorator uses) rather than
 *       a single registered bean.
 *   <li><b>Impersonation-gated.</b> The real Javadoc states this "will always
 *       return true for non-impersonated sources" — the check is only meaningfully
 *       consulted when the target source has user impersonation enabled. A source
 *       without impersonation configured will never call out to this platform's
 *       identity/authz layer at all, regardless of what's registered.
 *   <li><b>Caching happens upstream, in Dremio.</b> {@code PermissionCheckCache}
 *       (Dremio's own class) caches results by (username, NamespaceKey) with a
 *       configurable TTL and explicitly instructs implementations: "No caching
 *       should be done in the plugin." {@link com.dremioplatform.adapter.http.MaskingPolicyHttpClient}'s
 *       cache is fine (that's a different call path — masking, not this hook) but
 *       a {@code hasAccessPermission} implementation itself should call straight
 *       through to {@link com.dremioplatform.adapter.grpc.AuthCheckGrpcClient} on
 *       every invocation, not add a second cache layer.
 * </ol>
 *
 * This mirror interface keeps the platform's existing operation-string vocabulary
 * (used by {@link DremioAccessControlAdapter} and its gRPC call) rather than
 * switching to the real {@code NamespaceKey}/{@code DatasetConfig} types, since
 * those are dremio-oss protobuf/internal types this module can't compile against
 * without a local dremio-oss build (see module README). The reconciliation step
 * that IS still required: writing the actual {@code StoragePlugin} decorator or
 * custom source whose real {@code hasAccessPermission(String, NamespaceKey, DatasetConfig)}
 * calls {@link #hasAccess} (translating {@code NamespaceKey.toString()}-equivalent
 * path addressing into this platform's scope-path format).
 */
public interface DremioAccessControlChecker {

    /** True if the given principal may perform `operation` against `datasetPath`. */
    boolean hasAccess(String principalType, String principalId, String datasetPath, String operation);

    /** Bulk variant for plan-time checks against every table referenced in a query. */
    java.util.Map<String, Boolean> hasAccessBulk(String principalType, String principalId,
                                                   java.util.List<String> datasetPaths, String operation);
}
