package com.dremioplatform.toba.engine;

import java.util.UUID;

/**
 * Pluggable execution backend contract. Toba owns identity, authorization, audit,
 * workload admission and query-job ownership; an engine adapter only translates
 * those authorized requests to its provider protocol.
 */
public interface LakehouseEngine {
    /** Stable configured ID, e.g. "dremio-prod" or "trino-analytics". */
    String engineId();

    EngineCapabilities capabilities();

    EngineQueryHandle submit(EngineQuery query);

    EngineQueryStatus status(EngineQueryHandle handle);

    EngineResultPage fetchResults(EngineQueryHandle handle, int offset, int limit);

    /** Must fail explicitly when QUERY_CANCELLATION is not advertised. */
    void cancel(EngineQueryHandle handle);

    /** Optional profile surface; implementations must fail explicitly if unsupported. */
    String queryProfile(EngineQueryHandle handle);

    /** Health/readiness probe for engine registry and control-plane checks. */
    EngineHealth health();

    record EngineHealth(boolean reachable, String version, String safeMessage) { }
}
