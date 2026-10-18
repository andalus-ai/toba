# Reconciliation Notes: dremio-adapter vs. real dremio-oss

This document records actual research against the real `dremio-oss` source, done to
replace the earlier "mirror interfaces, unverified" caveat with verified findings
where possible. Methodology first, then findings.

## Methodology

`dremio-oss` isn't on Maven Central under consumable coordinates (see main README),
but its source **is** on GitHub, and a full clone isn't necessary just to read
specific files. This reconciliation pass used a shallow, blob-filtered clone:

```bash
git clone --filter=blob:none --depth 1 --no-checkout https://github.com/dremio/dremio-oss.git
```

`--filter=blob:none` fetches only commit/tree metadata up front (fast, small), not
file contents; individual file contents are then fetched on demand with
`git cat-file -p HEAD:<path>` only for files actually needed. This is a much lighter
weight approach than a full `mvn clean install` checkout when the goal is reading
specific interface signatures, not building the whole project.

Commit inspected: `HEAD` of the default branch at the time of this research
(exact commit not pinned here — re-running the clone command will pick up whatever
is current; if these findings and current `dremio-oss` HEAD disagree, trust the repo).

## Finding 1: The real access-control hook is `StoragePlugin.hasAccessPermission`, not a standalone checker

**File:** `sabot/kernel/src/main/java/com/dremio/exec/store/StoragePlugin.java`

```java
public interface StoragePlugin extends Service, SourceMetadata, Wrapper {
  /**
   * Whether the given user can access the entity at the given location according to the
   * underlying source. This will always return true for non-impersonated sources. For
   * impersonated sources this will consult the underlying source. No caching should be
   * done in the plugin. If dataset config doesn't have complete information needed to
   * check access permission always return true.
   */
  boolean hasAccessPermission(String user, NamespaceKey key, DatasetConfig datasetConfig);
  // ... many other methods (StoragePlugin is a large interface)
}
```

- `NamespaceKey` = `com.dremio.service.namespace.NamespaceKey` (module `services/namespace`)
- `DatasetConfig` = `com.dremio.service.namespace.dataset.proto.DatasetConfig` (protobuf type)

**Caller:** `sabot/kernel/src/main/java/com/dremio/exec/catalog/PermissionCheckCache.java`
— caches `hasAccessPermission` results by `(username, NamespaceKey)` with a
configurable TTL (`authTtlMs`), and its own Javadoc reiterates "No caching should be
done in the plugin" — i.e. Dremio owns the caching layer; an adapter implementation
should call straight through on every invocation.

**What this changes vs. the plan's original assumption** (and vs. this module's
original, unverified `DremioAccessControlChecker` mirror):

1. **Per-source, not global.** There's no single bean to register cluster-wide —
   each `StoragePlugin` instance (one per registered source) has its own
   `hasAccessPermission`. Real integration means a custom `StoragePlugin`
   implementation or a decorator around each source you want governed, not one
   central registration.
2. **Impersonation-gated.** "Always return true for non-impersonated sources" means
   this hook is a no-op unless the target source has Dremio's own user
   impersonation feature turned on. That's an operational prerequisite, not
   something this adapter can work around.
3. **Related, adjacent finding:** dremio-oss also has an internal `Catalog`-level
   decorator, `sabot/kernel/src/main/java/com/dremio/exec/catalog/SourceAccessChecker.java`,
   which blocks access to internal system sources (`__logs`, etc.) — this is a
   *different* mechanism (blocks visibility of certain sources entirely) from
   per-user `hasAccessPermission`, and not a general-purpose RBAC hook. Worth
   knowing about but not what this adapter should target.

**Still not done:** actually writing the `StoragePlugin` decorator/implementation.
`StoragePlugin` is a large interface (many methods beyond `hasAccessPermission`);
a real decorator needs to delegate every other method to a wrapped instance,
following the same pattern `SourceAccessChecker` itself uses for `Catalog`. That's
real implementation work requiring a local `dremio-oss` build to compile against —
this reconciliation pass identified *what* to build, not *the built thing*.

## Finding 2: Row/column masking is edition-gated at the handler level in OSS — confirming, not just assuming, the plan's approach

**Files:**
`sabot/kernel/src/main/java/com/dremio/exec/planner/sql/parser/SqlAlterTableSetColumnMasking.java`,
`SqlAlterTableUnsetColumnMasking.java`

OSS ships the **full SQL grammar** for native masking policies:

```sql
CREATE [OR REPLACE] COLUMN POLICY <policy_name> AS (<field> <type>, ...)
  RETURN <type> <policy_sql_expression>

ALTER TABLE <table> MODIFY COLUMN <col> SET MASKING POLICY <policy_name>
```

But `SqlAlterTableSetColumnMasking.toDirectHandler(...)` — the method that would
actually *execute* this DDL — does this:

```java
try {
  final Class<?> cl = Class.forName(
      "com.dremio.exec.planner.sql.handlers.EnterpriseAlterTableSetColumnMasking");
  final Constructor<?> ctor = cl.getConstructor(QueryContext.class);
  return (SimpleDirectHandler) ctor.newInstance(context);
} catch (...) {
  throw UserException.unsupportedError()
      .message("This command is not supported in this edition of Dremio.")
      .buildSilently();
}
```

`com.dremio.exec.planner.sql.handlers.EnterpriseAlterTableSetColumnMasking` **does
not exist anywhere in the `dremio-oss` source tree.** It's loaded reflectively so
Enterprise builds (which do include that class) get real execution, while OSS
builds hit `ClassNotFoundException` and fail with "not supported in this edition."

**What this confirms:** the plan's Section 9 approach — "enforced by rewriting the
logical plan (Calcite RelNode view wrapping) before Dremio's optimizer runs —
implemented in the adapter layer, not the coordinator itself" — is not merely *a*
reasonable design, it is **the only viable approach for OSS**, because there is no
native execution path to hook into or trigger; it's architecturally absent from the
OSS jar, not just unused or defaulted off. This is a meaningfully stronger claim
than the plan document originally made, and worth carrying back into that document.

**Still not done:** the actual Calcite `RelNode` rewrite. Confirming *why* the
adapter-side approach is necessary doesn't implement it — that still needs
Calcite's `RelBuilder` and Dremio's plan-building context, unavailable without a
local `dremio-oss` build. See `MaskingPolicyProvider`'s Javadoc for the exact scope
boundary of what this codebase does (resolve applicable rules) vs. doesn't (rewrite
the plan).

## Finding 3: There IS a real plugin mechanism for custom functions — but it conflicts architecturally with per-row network calls

**Files:**
`sabot/kernel/src/main/java/com/dremio/exec/expr/fn/PluggableFunctionRegistry.java`,
`FunctionImplementationRegistry.java`, `SimpleFunction.java`,
`annotations/FunctionTemplate.java`

Unlike access-control and masking, custom scalar functions in dremio-oss **do**
have a genuine, working, classpath-scanned plugin mechanism:

```java
public interface PluggableFunctionRegistry {
  List<SqlOperator> listOperators(boolean isDecimalV2Enabled);
  AbstractFunctionHolder findFunction(FunctionCall functionCall);
}
```

`FunctionImplementationRegistry.generatePluggableFunctionRegistries` does
`classpathScan.getImplementations(PluggableFunctionRegistry.class)` — a genuine
classpath scan (the same mechanism that already picks up `HiveFunctionRegistry` and
`GandivaFunctionRegistry` as built-in examples) that discovers and instantiates any
`PluggableFunctionRegistry` implementation found on the classpath via Guice
injection. **A jar dropped onto Dremio's classpath implementing this interface is
picked up automatically at startup** — this is a real, working extension point, not
edition-gated like masking.

**The conflict:** the actual per-row function body contract is:

```java
public interface SimpleFunction extends Function {
  void setup();
  void eval();
}
```

`eval()` takes no parameters and returns nothing — field access happens through
`@Param`/`@Output`/`@Workspace`-annotated fields that Dremio's codegen framework
splices into a generated class body (via Janino) at plan time, then executes
**per record, in a tight loop, on the calling execution thread**. There is no
batching hook, no async contract, nothing resembling `Uni<T>` or a callback — it's
low-level, JIT-friendly, synchronous-by-construction row evaluation.

**What this means for `AI_SUMMARIZE`/`AI_CLASSIFY`:** implementing these as a true
native `SimpleFunction` that calls out to `semantic-ai-service` per row is
*technically possible* via `PluggableFunctionRegistry` — nothing blocks it — but it
would mean **blocking network I/O on the execution thread, once per row**, inside a
loop designed for branch-predictable, allocation-free arithmetic. Under any
non-trivial result set this would serialize every AI-function call onto whatever
thread is evaluating that batch, stall the executor, and likely blow through query
timeouts long before it blew through Anthropic's own rate limits. `FunctionTemplate`
even has a `costCategory()` field (`SIMPLE`/`MEDIUM`/`COMPLEX`, with `COMPLEX`
costed at 50x `SIMPLE` for the planner) — which at least lets the optimizer be told
this function is expensive, but doesn't change the per-row blocking-call problem.

**Conclusion, mirroring Finding 2's shape:** the plugin mechanism is real and
usable, but the plan's implicit assumption (an `AI_SUMMARIZE` UDF that calls this
platform's Semantic AI Service like a normal SQL function) runs into a genuine
architectural mismatch, not a missing feature. The pragmatic path — implemented in
this pass, see `dremio-adapter`'s `spi/DremioAiFunctionRegistry.java` and
`ai/AiFunctionHttpClient.java` — is: build the real `PluggableFunctionRegistry`
wiring (since it's genuinely available), but scope its use explicitly to small,
interactive, ad-hoc queries via a documented warning, add an in-memory
response cache keyed by input-text hash (many real datasets have highly repeated
categorical text, so this isn't a token gesture), and explicitly recommend against
its use over large batch/ETL-scale result sets — that use case needs a
pre-computation pipeline (write `AI_SUMMARIZE` results into a materialized column
ahead of query time), not a per-row scalar function, regardless of how it's wired.

Everything in `dremio-adapter` outside these two findings — most notably, the exact
mechanism for registering a custom `StoragePlugin`/decorator with a running Dremio
coordinator (plan Section 7 names this only as "thin adapter jar registered via
Dremio's plugin mechanism," and this research pass didn't chase that thread down).
That, and the Workload Orchestrator's executor-provisioning integration point
(plan Section 7's `provision` module row), are the next candidates for the same
clone-and-read treatment if/when they matter more than they currently block anything.
