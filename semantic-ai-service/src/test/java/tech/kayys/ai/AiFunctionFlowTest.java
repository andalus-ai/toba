package com.dremioplatform.ai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AiFunctionFlowTest {

    @Test
    void summarize_returnsResultAndModel() {
        given().contentType(ContentType.JSON)
                .header("X-Principal-Type", "USER")
                .header("X-Principal-Id", "test-user-1")
                .body("{\"inputText\": \"The quarterly report shows revenue grew 12% year over year.\"}")
                .when().post("/api/v1/ai-functions/summarize")
                .then()
                .statusCode(200)
                .body("result", notNullValue())
                .body("model", equalTo("mock-chat-v1"));
    }

    @Test
    void classify_withoutCategories_returns400() {
        given().contentType(ContentType.JSON)
                .header("X-Principal-Type", "USER")
                .header("X-Principal-Id", "test-user-1")
                .body("{\"inputText\": \"Some text to classify\"}")
                .when().post("/api/v1/ai-functions/classify")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("AI-400-CATEGORIES-REQUIRED"));
    }

    @Test
    void classify_withCategories_returnsResult() {
        given().contentType(ContentType.JSON)
                .header("X-Principal-Type", "USER")
                .header("X-Principal-Id", "test-user-1")
                .body("{\"inputText\": \"Customer complained about a late delivery\", \"categories\": [\"complaint\", \"praise\", \"question\"]}")
                .when().post("/api/v1/ai-functions/classify")
                .then()
                .statusCode(200)
                .body("result", notNullValue());
    }
}
