# Dremio Adapter

The piece that actually connects Dremio OSS to the rest of this repo (plan Section 7).
Not a deployable service — a plugin jar meant to run **inside** Dremio's own
coordinator JVM.

## Status

| # | Layer | Status |
|---|---|---|
| gRPC client (`grpc/`) → identity-access-service's AuthCheckService | ✅ done |
| HTTP client (`http/`) → governance-lineage-service's masking-policy lookup | ✅ done |
| HTTP client (`ai/`) → semantic-ai-service's AI_SUMMARIZE/AI_CLASSIFY, with response caching | ✅ done — see RECONCILIATION_NOTES.md Finding 3 for the real (and constrained) plugin mechanism this targets |
| SPI mirror interfaces (`spi/`) + adapters wiring them to the clients above | ✅ done; **verified** against real dremio-oss source (see RECONCILIATION_NOTES.md) — not the actual `StoragePlugin`/`PluggableFunctionRegistry` implementations yet |
| Real Calcite `RelNode` masking rewrite | not started — out of scope without the real SPI, see below |
| Real dremio-oss SPI reconciliation | 🟡 partial — access control and masking are researched and cited (Findings 1 & 2); function-registry plugin mechanism is also researched (Finding 3), but none of the three real Dremio-side classes are implemented yet |
| Tests | 🟡 started — `DremioAccessControlAdapter` and `AiFunctionHttpClient` both have real unit tests (mocked gRPC client; a real embedded `HttpServer` for the AI client); `MaskingPolicyHttpClient`'s parser and `DremioMaskingPolicyAdapter` don't yet |

## ⚠️ `spi/` reconciliation status: partially verified against real dremio-oss

**Update:** two of `spi/`'s two interfaces have now been checked against real
`dremio-oss` source (not just guessed from the plan document) — see
`RECONCILIATION_NOTES.md` for full detail, methodology, and citations. Summary:

- **Access control**: the real hook is `StoragePlugin.hasAccessPermission(String, NamespaceKey, DatasetConfig)`
  — per-source (not a global registered checker), impersonation-gated, with caching
  owned by Dremio itself. `DremioAccessControlChecker`'s Javadoc now cites this
  directly. Still not done: the actual `StoragePlugin` decorator implementation
  (a large interface to delegate) — this pass identified *what* to build, not the
  built thing.
- **Masking**: OSS parses the native masking DDL (`CREATE COLUMN POLICY`,
  `ALTER TABLE ... SET MASKING POLICY`) but its execution handler class
  (`EnterpriseAlterTableSetColumnMasking`) doesn't exist in OSS at all — confirmed
  by reading the exact `Class.forName(...)` reflection fallback that throws
  "not supported in this edition of Dremio." This means the plan's adapter-side
  Calcite-plan-rewrite approach isn't just *a* reasonable design, it's the **only**
  viable one for OSS. `MaskingPolicyProvider`'s Javadoc now cites this directly.
- **Custom functions (for `AI_SUMMARIZE`/`AI_CLASSIFY`)**: unlike the two findings
  above, there IS a genuine, working, classpath-scanned plugin mechanism —
  `PluggableFunctionRegistry`, auto-discovered via `ScanResult.getImplementations(...)`,
  the same way `HiveFunctionRegistry` is. **But** the real per-row execution
  contract (`SimpleFunction.eval()`) is synchronous, per-row, on the execution
  thread, with no batching/async hook — making a blocking network call per row
  inside it is architecturally *possible* but operationally dangerous at scale.
  `DremioAiFunctionRegistry`'s Javadoc and `ai/AiFunctionHttpClient`'s response
  cache (keyed by input-text hash, tested against a real embedded `HttpServer` in
  `AiFunctionHttpClientTest`) exist to make small/interactive use viable without
  pretending the batch-scale problem is solved — see RECONCILIATION_NOTES.md
  Finding 3 for the full reasoning.

**Still fully unverified:** how a custom `StoragePlugin`/decorator actually gets
registered with a running Dremio coordinator (plan Section 7 names this only at
the level of "thin adapter jar registered via Dremio's plugin mechanism"). That's
the next thread to pull if/when it blocks something concrete.

**Why this was possible without a full local build:** `dremio-oss`'s source is
public on GitHub even though its build artifacts aren't on Maven Central. A
shallow, blob-filtered clone (`git clone --filter=blob:none --depth 1 --no-checkout`)
fetches tree metadata without downloading file contents up front, then
`git cat-file -p HEAD:<path>` reads specific files on demand — enough to verify
interface signatures and trace a reflection-based edition gate without a full
`mvn clean install` of the whole monorepo. See RECONCILIATION_NOTES.md for the
exact commands.

## What IS solid regardless of that reconciliation

- **`AuthCheckGrpcClient`** — calls identity-access-service's real, already-built
  `AuthCheckService` gRPC contract (copied proto, kept in sync by hand — see the
  comment at the top of `src/main/proto/auth_check.proto`). Fails closed (deny) on
  any timeout/network error, matching the server-side fail-closed behavior in
  `AuthCheckServiceImpl`.
- **`MaskingPolicyHttpClient`** — calls governance-lineage-service's real, already-
  built `GET /api/v1/masking-policies/applicable` endpoint. Fails **open to an
  exception**, not to "no policies" — see its Javadoc for why that's the opposite
  fail-direction from the access-control client, and deliberate.
- **Why this jar has no Quarkus dependency** — it loads inside Dremio's coordinator
  JVM and shares its classpath. Quarkus's dependency tree (or any large one) risks
  version conflicts with whatever Dremio itself pins. gRPC/protobuf are shaded and
  relocated (`maven-shade-plugin` config in `pom.xml`) for the same reason — Dremio
  already uses gRPC internally for its own RPC fabric (plan Section 2), so an
  unshaded copy here would very likely collide.
- **Why the masking-policy JSON parser is hand-rolled regex, not a JSON library**
  — same classpath-minimalism reasoning. Documented in `MaskingPolicyHttpClient`'s
  Javadoc as a known simplification to revisit if the dependency budget allows a
  real JSON library later.

## Wiring this into a real Dremio OSS build

1. Read `RECONCILIATION_NOTES.md` first — the access-control and masking hooks are
   now identified precisely (real class/method/file), which changes step 2 below
   from "go find the interface" to "go implement against the interface already
   named here."
2. Implement the real `StoragePlugin` decorator (or custom source) whose
   `hasAccessPermission(String, NamespaceKey, DatasetConfig)` calls
   `DremioAccessControlAdapter.hasAccess(...)` — this needs a local `dremio-oss`
   checkout to compile against `StoragePlugin`, `NamespaceKey`, and `DatasetConfig`.
3. Build this module: `mvn -pl dremio-adapter -am install` — produces a shaded jar
   with `AuthCheckGrpcClient`'s gRPC/protobuf dependencies relocated.
4. Register the decorator per whatever mechanism your dremio-oss version uses for
   custom `StoragePlugin`s — this specific step is still unverified (see
   RECONCILIATION_NOTES.md "What's still unverified").
5. Point `dremio-adapter.properties` (or the `dremio.adapter.*` system property
   overrides — see `AdapterConfig`) at your running identity-access-service,
   governance-lineage-service, and semantic-ai-service instances.
6. Call `AdapterFactory.create()` from wherever Dremio's plugin lifecycle
   instantiates your registered checker, and call `.close()` on coordinator shutdown.
   For `AI_SUMMARIZE`/`AI_CLASSIFY` specifically: implement a real
   `PluggableFunctionRegistry` (genuinely simpler than the `StoragePlugin` piece —
   see RECONCILIATION_NOTES.md Finding 3) whose `SimpleFunction.eval()` calls
   `AdapterFactory.aiFunctionRegistryAdapter()`, and read that finding's
   blocking-I/O-at-scale warning before deciding where to actually expose these
   functions.

## Running the tests

```bash
mvn -pl dremio-adapter -am test
```

No Testcontainers/Dev Services needed here — `DremioAccessControlAdapterTest` mocks
`AuthCheckGrpcClient` directly, and `AiFunctionHttpClientTest` spins up a real (tiny)
`com.sun.net.httpserver.HttpServer` rather than standing up a full
identity-access-service or semantic-ai-service instance.

## Next continuation prompt

**"Add unit tests for MaskingPolicyHttpClient's response parser"** (the regex-based
JSON reader is exactly the kind of code that benefits from edge-case tests — escaped
quotes, null columnName, multiple policies in one response) — or, if a local
dremio-oss checkout becomes available, **"Implement the real StoragePlugin decorator
and PluggableFunctionRegistry against dremio-oss"** — both interfaces are now
precisely identified (RECONCILIATION_NOTES.md, Findings 1 and 3), so this is
"go build it" work, not "go find it" research.
