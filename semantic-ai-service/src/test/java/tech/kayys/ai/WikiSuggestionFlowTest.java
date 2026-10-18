package com.dremioplatform.ai;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WikiSuggestionFlowTest {

    @Test
    void generate_thenApprove_transitionsStatusAndRecordsReviewer() {
        String datasetPath = "space.hr.employees_" + UUID.randomUUID();
        String genBody = """
                {"datasetPath": "%s", "context": "Columns: employee_id (int), hire_date (date), department (string)"}
                """.formatted(datasetPath);

        String id = given().contentType(ContentType.JSON).body(genBody)
                .when().post("/api/v1/wiki-suggestions")
                .then().statusCode(201)
                .body("status", equalTo("PENDING"))
                .body("suggestedDescription", notNullValue())
                .extract().path("id");

        String actorId = UUID.randomUUID().toString();
        given().contentType(ContentType.JSON).header("X-Actor-Id", actorId).body("""
                {"approve": true, "reviewNote": "Looks accurate"}
                """)
                .when().put("/api/v1/wiki-suggestions/" + id + "/review")
                .then().statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("reviewedByUserId", equalTo(actorId));
    }

    @Test
    void review_alreadyReviewedSuggestion_returns409() {
        String datasetPath = "space.hr.payroll_" + UUID.randomUUID();
        String id = given().contentType(ContentType.JSON).body("""
                {"datasetPath": "%s", "context": "Columns: amount, pay_date"}
                """.formatted(datasetPath))
                .when().post("/api/v1/wiki-suggestions")
                .then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).header("X-Actor-Id", UUID.randomUUID().toString())
                .body("{\"approve\": false}")
                .when().put("/api/v1/wiki-suggestions/" + id + "/review")
                .then().statusCode(200);

        given().contentType(ContentType.JSON).header("X-Actor-Id", UUID.randomUUID().toString())
                .body("{\"approve\": true}")
                .when().put("/api/v1/wiki-suggestions/" + id + "/review")
                .then().statusCode(409)
                .body("errorCode", equalTo("AI-409-SUGGESTION-ALREADY-REVIEWED"));
    }

    @Test
    void pending_listOnlyIncludesUnreviewedSuggestions() {
        String datasetPath = "space.ops.list_test_" + UUID.randomUUID();
        given().contentType(ContentType.JSON).body("""
                {"datasetPath": "%s", "context": "some context"}
                """.formatted(datasetPath))
                .when().post("/api/v1/wiki-suggestions")
                .then().statusCode(201);

        given().queryParam("datasetPath", datasetPath)
                .when().get("/api/v1/wiki-suggestions")
                .then().statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].status", equalTo("PENDING"));
    }
}
