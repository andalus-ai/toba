# Semantic AI Service

Phase 5 of the roadmap in `dremio-reactive-extension-plan.md`. Owns dataset/column
embeddings + semantic search, AI-generated wiki suggestions (human-approved, never
auto-applied), SQL-callable AI functions, and an MCP tool surface for agent access.

## Status

| # | Layer | Status |
|---|---|---|
| 1 | Domain model + migration (pgvector) | ✅ done |
| 2 | Repository layer (incl. native cosine-similarity search) | ✅ done |
| 3 | Service layer (embeddings, wiki suggestions, AI functions) | ✅ done, plus a real optional OpenAI embedding provider (`ai.embedding.provider=openai`) |
| 4 | REST layer | ✅ done |
| 5 | Kafka consumer: auto-embed new tables from `catalog.events` | ✅ done (minimal — see gaps) |
| 6 | MCP tool surface | ✅ done — real MCP server, authenticated + per-tool authorized via identity-access-service's AuthCheckService |
| 7 | Tests | ✅ started — embedding round-trip/search, wiki-suggestion review workflow, AI function calls |

## ⚠️ Read this before assuming "AI" here means "real AI" by default

**Chat completion is real** (`AnthropicChatClient`, backed by `quarkus-langchain4j-anthropic`)
— used for `AI_SUMMARIZE`, `AI_CLASSIFY`, and wiki-suggestion generation, when
`ai.mock-mode=false` and `ANTHROPIC_API_KEY` is set.

**Embeddings can now be real too, opt-in, via a different vendor.** Anthropic does
not publish a public embeddings API, so there's no "AnthropicEmbeddingClient" —
instead, `OpenAiEmbeddingClient` (backed by `quarkus-langchain4j-openai`,
`text-embedding-3-small`, 1536 dimensions) is available and used whenever
`ai.embedding.provider=openai` and `OPENAI_API_KEY` is set. This is controlled by
its *own* config flag, independent of `ai.mock-mode` — chat and embeddings can be
real independently of each other. **The default is still mock** (`ai.embedding.provider=mock`)
so local dev/tests never need any API key. See `LlmClient`'s and `RoutingLlmClient`'s
Javadoc for exactly how the two flags combine, and note the important operational
caveat: switching embedding providers after data is already embedded requires
re-embedding everything — vectors from different models aren't comparable, and
`MockLlmClient`'s embeddings are never real (deterministic hash-based pseudo-vectors,
useful for exercising the pipeline mechanics, not for real semantic search quality).

## What's built

- **`DatasetEmbedding`** — deliberately does NOT map the `embedding` pgvector column
  through JPA (Hibernate ORM has no native `vector` type). `DatasetEmbeddingRepository`
  writes/reads it via hand-written native SQL (`::vector` cast, `<=>` cosine-distance
  operator) in the same transaction as the normal Panache-mapped columns.
- **`EmbeddingService.upsert`** — embeds text *outside* the transaction (an LLM call
  has no business holding a DB transaction open), then persists/updates the row and
  writes the vector in one transaction.
- **`WikiSuggestion`** — AI-proposed descriptions, `PENDING` until a human calls
  `PUT .../review`. This service never writes a suggestion into Dremio's actual wiki
  metadata itself — that stays a separate, human-triggered action by design (plan
  Section 5.5, "keeps humans in the loop").
- **`AiFunctionInvocation`** — every `AI_SUMMARIZE`/`AI_CLASSIFY` call is logged,
  including failures, for cost/usage tracking and so Governance's audit trail can
  eventually answer "did an AI function ever see this column's data."
- **`CatalogEventConsumer`** — auto-embeds new tables on `catalog.events`
  `TABLE_CREATED`, though with a minimal placeholder description (just the
  namespace/name path) — see "Known gaps" for the real-schema enrichment this needs.
- **MCP endpoint authentication** — `quarkus-smallrye-jwt` verifies the same
  platform JWTs `identity-access-service`'s `TokenService` issues (same issuer,
  same public key in dev). `/mcp` and `/mcp/*` require a valid Bearer token
  (`quarkus.http.auth.permission.mcp`). The `/api/v1` REST surface now also
  requires an authenticated JWT (`quarkus.http.auth.permission.rest`). This is a
  standard Quarkus Security path-permission rule, not custom auth code — verified
  against the extension's actual sample (`quarkiverse/quarkus-mcp-server`'s
  `secure-mcp-sse-server` sample uses the same `quarkus-oidc`/JWT + path-permission
  pattern) before wiring it in.
- **Per-tool MCP authorization** — `McpAuthorizationService` calls
  identity-access-service's real `AuthCheckService` gRPC contract (the same one
  `dremio-adapter` calls, via a third client-side copy of `auth_check.proto` — see
  that proto's header comment on the platform-bom follow-up that would eliminate
  the copy-paste) to gate both tools with `catalog.dataset.read`.
  `get_dataset_embeddings` checks up front (it always has one target path);
  `search_datasets` can't (free-text query, no single scope path) so it
  post-filters results to only paths the caller may read. Fails closed (deny) on
  any gRPC error, mirroring `dremio-adapter`'s own fail-closed convention. Errors
  use `io.quarkiverse.mcp.server.ToolCallException` — verified against the
  extension's real error-handling docs, since `@Tool` methods aren't JAX-RS
  resources and never reach `AiExceptionMapper`.
- **`SemanticCatalogMcpTools`** — a real MCP server, via the verified, current
  `io.quarkiverse.mcp:quarkus-mcp-server-http` extension (v1.13.0, stable). Two
  `@Tool`-annotated methods (`searchDatasets`, `getDatasetEmbeddings`) are
  discovered and JSON-Schema-generated at build time — no hand-written schema, no
  manual JSON-RPC wiring. Served over Streamable HTTP at `/mcp` (legacy SSE at
  `/mcp/sse`) as soon as the service starts; connect any MCP client (Claude, an
  agent framework, or the extension's own Dev UI at `/q/dev-ui` in `quarkus:dev`)
  directly. Confirmed by reading the extension's actual documentation (return-type
  support for `Uni<T>`, default transport paths) rather than assumed.

## Known gaps / next continuation prompts

- **`CatalogEventConsumer`'s embedded text is minimal.** A real pipeline would call
  catalog-service's `GET .../tables/{table}` to pull actual schema/properties and
  embed a richer description — not implemented here since it needs a new HTTP
  client dependency this increment doesn't add.
- **`TABLE_DROPPED` events don't clean up embeddings.** Dropped tables leave stale
  embedding rows behind — a correctness gap for search result quality over time.
- **No Dremio UDF wiring — partially closed.** `AI_SUMMARIZE`/`AI_CLASSIFY` have a
  working REST backend (`AiFunctionResource`) AND `dremio-adapter` now has a real,
  tested HTTP client (`ai/AiFunctionHttpClient`, with input-hash-keyed response
  caching) that calls it. What's still missing is the actual Dremio-side
  `SimpleFunction`/`PluggableFunctionRegistry` implementation that would make
  `AI_SUMMARIZE` callable from SQL — that needs a local `dremio-oss` checkout to
  compile against real Calcite/Dremio function types (see
  `dremio-adapter/RECONCILIATION_NOTES.md`, Finding 3, for exactly what's needed
  and — importantly — a documented architectural warning: the real per-row
  function-execution contract is synchronous and blocking, so this is only
  recommended for small/interactive queries, not batch/ETL-scale result sets,
  regardless of how well the plugin wiring itself works.
- **MCP authorization is discovery-only, read-scoped.** Both tools check
  `catalog.dataset.read` — there's no write/manage-scoped tool yet, so this covers
  the two tools that exist, not a general framework for future ones.
- **`search_datasets`' authorization is post-filter, not pre-filter.** The
  embedding search still runs (and costs an embedding call) before results are
  filtered down to what the caller can see — fine for the current scale, but worth
  knowing if this ever needs to scope the vector search itself for efficiency.
- **No integration test for the full MCP-to-gRPC authorization path** —
  `McpAuthorizationServiceTest` is a plain unit test with a mocked gRPC client;
  nothing exercises the real `/mcp` endpoint end-to-end against a live (or
  test-doubled) identity-access-service.
- **REST API (`/api/v1/**`) now requires authentication**, but this is only
  the first security boundary. Per-resource permission checks, workspace/tenant
  scoping, and trusted service identities are not yet uniformly enforced. Do not
  treat authenticated access as authorization to any dataset or AI function.
- **Only 2 of the plan's "discover datasets and run governed queries" MCP tools
  exist** — `searchDatasets` and `getDatasetEmbeddings` cover discovery.
  "Run governed queries" is deliberately not a tool here — see
  `SemanticCatalogMcpTools`'s trailing note for why that responsibility belongs
  elsewhere (likely `dremio-adapter`, to reuse its existing AuthCheckService wiring
  rather than duplicating authz logic in this service).
- **No budget/rate limiting on AI function calls** — `AiFunctionInvocationRepository.sumTokensForPrincipal`
  exists for this but nothing enforces a limit yet.
- **No embedding-provider-change migration tooling.** Switching `ai.embedding.provider`
  requires manually re-embedding every existing row (see `RoutingLlmClient`'s
  Javadoc) — there's no bulk "re-embed everything" endpoint or job yet.

## Running locally

Quarkus Dev Services' default Postgres image lacks the `pgvector` extension this
service's migration needs — `application.properties` overrides the dev/test image
to `pgvector/pgvector:pg16` specifically for this module.

```bash
mvn -pl semantic-ai-service -am quarkus:dev
```

Runs entirely in mock mode by default (`ai.mock-mode=true`, `ai.embedding.provider=mock`)
— no API key needed to exercise any endpoint. To test against real providers:

```bash
# Real chat only:
AI_MOCK_MODE=false ANTHROPIC_API_KEY=sk-ant-... mvn -pl semantic-ai-service -am quarkus:dev

# Real chat + real embeddings:
AI_MOCK_MODE=false ANTHROPIC_API_KEY=sk-ant-... \
  AI_EMBEDDING_PROVIDER=openai OPENAI_API_KEY=sk-... \
  mvn -pl semantic-ai-service -am quarkus:dev
```

**MCP:** once running, the tool server is live at `http://localhost:8080/mcp`
(Streamable HTTP) — connect the MCP Inspector, Claude, or any MCP client directly,
or test from the browser via the Dev UI's "MCP Server" card at
`http://localhost:8080/q/dev-ui` while in `quarkus:dev`.

## Next continuation prompt

**"Add an MCP integration test exercising the real /mcp endpoint against a
test-doubled AuthCheckService"** — `McpAuthorizationServiceTest` covers the authz
logic in isolation; nothing yet drives an actual MCP JSON-RPC call through the
live endpoint to confirm the wiring end-to-end (would likely use the extension's
own McpAssured testing library, not yet added as a test dependency).
