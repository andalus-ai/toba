package com.dremioplatform.ai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Runs entirely against MockLlmClient (ai.mock-mode defaults to true) — deterministic
 * hash-based vectors, not real semantic similarity. See MockLlmClient's Javadoc.
 * These tests check the pipeline mechanics (upsert, search returns results, exact-
 * text-match self-similarity), not embedding *quality*, which the mock can't provide.
 */
@QuarkusTest
class EmbeddingFlowTest {

    @Test
    void upsertEmbedding_thenListForDataset_roundTrips() {
        String datasetPath = "space.sales.orders_" + UUID.randomUUID();
        String body = """
                {"datasetPath": "%s", "targetType": "TABLE", "sourceText": "Order transactions table"}
                """.formatted(datasetPath);

        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/v1/embeddings")
                .then().statusCode(200)
                .body("datasetPath", equalTo(datasetPath))
                .body("embeddingModel", equalTo("mock-embedding-v1"));

        given().queryParam("datasetPath", datasetPath)
                .when().get("/api/v1/embeddings")
                .then().statusCode(200)
                .body("size()", equalTo(1));
    }

    @Test
    void search_forExactlyEmbeddedText_returnsItAsTopMatchWithHighSimilarity() {
        String datasetPath = "space.finance.revenue_" + UUID.randomUUID();
        String sourceText = "Quarterly revenue by region and product line " + UUID.randomUUID();

        given().contentType(ContentType.JSON).body("""
                {"datasetPath": "%s", "targetType": "TABLE", "sourceText": "%s"}
                """.formatted(datasetPath, sourceText))
                .when().post("/api/v1/embeddings")
                .then().statusCode(200);

        // Mock embeddings are a deterministic function of exact input text, so
        // searching for the identical text must return a similarity of ~1.0.
        given().contentType(ContentType.JSON).body("""
                {"query": "%s", "limit": 5}
                """.formatted(sourceText))
                .when().post("/api/v1/embeddings/search")
                .then().statusCode(200)
                .body("[0].datasetPath", equalTo(datasetPath))
                .body("[0].similarityScore", greaterThan(0.99f));
    }

    @Test
    void upsert_existingTarget_updatesInPlaceRatherThanDuplicating() {
        String datasetPath = "space.ops.events_" + UUID.randomUUID();
        String createBody = """
                {"datasetPath": "%s", "targetType": "TABLE", "sourceText": "v1 description"}
                """.formatted(datasetPath);
        given().contentType(ContentType.JSON).body(createBody)
                .when().post("/api/v1/embeddings")
                .then().statusCode(200);

        String updateBody = """
                {"datasetPath": "%s", "targetType": "TABLE", "sourceText": "v2 description, revised"}
                """.formatted(datasetPath);
        given().contentType(ContentType.JSON).body(updateBody)
                .when().post("/api/v1/embeddings")
                .then().statusCode(200)
                .body("sourceText", equalTo("v2 description, revised"));

        given().queryParam("datasetPath", datasetPath)
                .when().get("/api/v1/embeddings")
                .then().statusCode(200)
                .body("size()", equalTo(1)); // still one row, not two
    }
}
