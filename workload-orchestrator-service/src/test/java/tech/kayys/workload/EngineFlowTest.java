package com.dremioplatform.workload;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Runs with workload.k8s.enabled=false (test default inherited from application.properties'
 * base value, since %test doesn't override it) — K8sScalingClient's no-op branch is
 * exercised here, not a real cluster. See that class's Javadoc.
 */
@QuarkusTest
class EngineFlowTest {

    @Test
    void createEngine_thenStart_thenStop_transitionsStatusAndReplicas() {
        String name = "engine_" + UUID.randomUUID();
        String body = """
                {
                  "name": "%s",
                  "minReplicas": 0,
                  "maxReplicas": 5,
                  "autoStopEnabled": true,
                  "autoStopIdleMinutes": 15,
                  "k8sNamespace": "dremio-execution",
                  "k8sDeploymentName": "%s-executors"
                }
                """.formatted(name, name);

        String id = given().contentType(ContentType.JSON).body(body)
                .when().post("/api/v1/engines")
                .then().statusCode(201)
                .body("status", equalTo("STOPPED"))
                .body("currentReplicas", equalTo(0))
                .extract().path("id");

        given().when().put("/api/v1/engines/" + id + "/start")
                .then().statusCode(200)
                .body("status", equalTo("RUNNING"))
                .body("currentReplicas", equalTo(1));

        given().when().put("/api/v1/engines/" + id + "/stop")
                .then().statusCode(200)
                .body("status", equalTo("STOPPED"))
                .body("currentReplicas", equalTo(0));

        given().when().get("/api/v1/engines/" + id + "/scaling-history")
                .then().statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].eventType", equalTo("STOPPED")); // most recent first
    }

    @Test
    void scale_beyondMaxReplicas_returns400() {
        String name = "engine_" + UUID.randomUUID();
        String createBody = """
                {"name": "%s", "minReplicas": 0, "maxReplicas": 3, "autoStopEnabled": false,
                 "autoStopIdleMinutes": 15, "k8sNamespace": "ns", "k8sDeploymentName": "dep"}
                """.formatted(name);
        String id = given().contentType(ContentType.JSON).body(createBody)
                .when().post("/api/v1/engines")
                .then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).body("""
                {"targetReplicas": 10, "reason": "load test"}
                """)
                .when().put("/api/v1/engines/" + id + "/scale")
                .then()
                .statusCode(400)
                .body("errorCode", equalTo("WORKLOAD-400-EXCEEDS-MAX-REPLICAS"));
    }

    @Test
    void wlmRule_resolve_matchesGlobPattern() {
        String engineName = "etl_engine_" + UUID.randomUUID();
        String createEngineBody = """
                {"name": "%s", "minReplicas": 0, "maxReplicas": 3, "autoStopEnabled": false,
                 "autoStopIdleMinutes": 15, "k8sNamespace": "ns", "k8sDeploymentName": "dep"}
                """.formatted(engineName);
        String engineId = given().contentType(ContentType.JSON).body(createEngineBody)
                .when().post("/api/v1/engines")
                .then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).body("""
                {"name": "etl-rule", "engineId": "%s", "priority": 10, "queryTagPattern": "etl:*",
                 "maxConcurrentQueries": 20, "queueTimeoutSeconds": 120}
                """.formatted(engineId))
                .when().post("/api/v1/wlm-queue-rules")
                .then().statusCode(201);

        given().queryParam("queryTag", "etl:nightly-load")
                .when().get("/api/v1/wlm-queue-rules/resolve")
                .then().statusCode(200)
                .body("engineId", equalTo(engineId));

        given().queryParam("queryTag", "bi-tool:dashboard")
                .when().get("/api/v1/wlm-queue-rules/resolve")
                .then().statusCode(404);
    }
}
