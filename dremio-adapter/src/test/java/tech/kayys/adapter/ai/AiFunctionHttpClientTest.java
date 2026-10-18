package com.dremioplatform.adapter.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dremioplatform.adapter.config.AdapterConfig;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Uses a real (if tiny) {@link HttpServer} rather than mocking java.net.http's
 * HttpClient, since HttpClient itself is hard to mock meaningfully — this
 * exercises AiFunctionHttpClient exactly as it behaves against a real HTTP
 * endpoint, including its JSON body construction, response parsing, and caching.
 */
class AiFunctionHttpClientTest {

    private HttpServer server;
    private AtomicInteger callCount;
    private AiFunctionHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        callCount = new AtomicInteger(0);
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v1/ai-functions/summarize", exchange -> {
            callCount.incrementAndGet();
            respond(exchange, 200, "{\"result\":\"a concise summary\",\"model\":\"test-model\"}");
        });
        server.createContext("/api/v1/ai-functions/classify", exchange -> {
            callCount.incrementAndGet();
            respond(exchange, 200, "{\"result\":\"complaint\",\"model\":\"test-model\"}");
        });
        server.start();

        System.setProperty("dremio.adapter.semantic-ai.base-url", "http://localhost:" + server.getAddress().getPort());
        client = new AiFunctionHttpClient(AdapterConfig.loadDefault());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        System.clearProperty("dremio.adapter.semantic-ai.base-url");
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    void summarize_parsesResultFromResponse() {
        assertEquals("a concise summary", client.summarize("The quarterly report shows growth."));
    }

    @Test
    void classify_parsesResultFromResponse() {
        assertEquals("complaint", client.classify("Delivery was late", List.of("complaint", "praise")));
    }

    @Test
    void summarize_repeatedIdenticalInput_hitsCacheNotServer() {
        client.summarize("identical input text");
        client.summarize("identical input text");
        client.summarize("identical input text");

        assertEquals(1, callCount.get(), "Only the first call should reach the server; the rest should be cache hits");
    }

    @Test
    void summarize_differentInputs_bothReachServer() {
        client.summarize("first distinct input");
        client.summarize("second distinct input");

        assertEquals(2, callCount.get());
    }

    @Test
    void classify_cacheKeyIncludesCategories_differentCategoriesNotConflated() {
        String result1 = client.classify("ambiguous text", List.of("complaint", "praise"));
        assertNotNull(result1);
        int callsAfterFirst = callCount.get();

        // Same input text, different categories list -> must NOT be a cache hit,
        // since the valid answer set differs.
        client.classify("ambiguous text", List.of("urgent", "routine"));
        assertEquals(callsAfterFirst + 1, callCount.get());
    }

    @Test
    void summarize_serverError_throwsAiFunctionCallException() throws IOException {
        HttpServer brokenServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        brokenServer.createContext("/api/v1/ai-functions/summarize", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        brokenServer.start();
        try {
            System.setProperty("dremio.adapter.semantic-ai.base-url", "http://localhost:" + brokenServer.getAddress().getPort());
            AiFunctionHttpClient brokenClient = new AiFunctionHttpClient(AdapterConfig.loadDefault());
            assertThrows(AiFunctionHttpClient.AiFunctionCallException.class,
                    () -> brokenClient.summarize("this call should fail"));
        } finally {
            brokenServer.stop(0);
            System.setProperty("dremio.adapter.semantic-ai.base-url", "http://localhost:" + server.getAddress().getPort());
        }
    }
}
