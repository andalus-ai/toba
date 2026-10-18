package com.dremioplatform.ai;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Confirms the auth boundary added around the MCP endpoint: unauthenticated
 * requests to /mcp are rejected, a valid platform JWT (minted here with the
 * test-only signing key — see pom.xml/application.properties) is accepted, and
 * the REST API remains reachable without a token (that boundary is intentionally
 * NOT extended to /api/** in this increment — see module README).
 *
 * This does not exercise the full MCP JSON-RPC protocol handshake (that needs the
 * extension's McpAssured testing library, not yet added — see module README
 * "Known gaps") — it only confirms the HTTP-layer auth policy applies where
 * expected and nowhere else.
 */
@QuarkusTest
class McpSecurityTest {

    @Test
    void mcpEndpoint_withoutToken_rejected() {
        given()
                .contentType("application/json")
                .body("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"id\":1,\"params\":{}}")
                .when().post("/mcp")
                .then()
                .statusCode(401);
    }

    @Test
    void mcpEndpoint_withValidPlatformToken_notRejectedByAuthLayer() {
        String token = Jwt.issuer("https://identity.dremio-platform.internal")
                .subject("11111111-1111-1111-1111-111111111111")
                .upn("test-mcp-client")
                .claim("principalType", "SERVICE_ACCOUNT")
                .expiresIn(Duration.ofMinutes(5))
                .sign();

        // A 401 here would mean the auth layer rejected a valid token — anything
        // else (200, or an MCP-protocol-level 4xx from a malformed JSON-RPC body)
        // means authentication succeeded and the request reached the MCP handler.
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body("{\"jsonrpc\":\"2.0\",\"method\":\"initialize\",\"id\":1,\"params\":{}}")
                .when().post("/mcp")
                .then()
                .statusCode(org.hamcrest.Matchers.not(401));
    }

    @Test
    void restApi_withoutToken_stillReachable() {
        // Confirms the auth boundary is scoped to /mcp/* only in this increment —
        // see module README "Known gaps" on why REST isn't behind the same wall yet.
        given()
                .when().get("/api/v1/embeddings?datasetPath=nonexistent")
                .then()
                .statusCode(200);
    }
}
