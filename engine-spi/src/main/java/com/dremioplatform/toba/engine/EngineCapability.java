package com.dremioplatform.toba.engine;

/** Features an engine explicitly advertises; absence means unsupported. */
public enum EngineCapability {
    ASYNC_EXECUTION,
    QUERY_CANCELLATION,
    PAGINATED_RESULTS,
    QUERY_PROFILE,
    PARAMETERIZED_SQL,
    CATALOG_DISCOVERY,
    EXPLAIN_PLAN,
    TRANSACTIONS,
    TIME_TRAVEL
}
