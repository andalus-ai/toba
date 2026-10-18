package com.dremioplatform.workload;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Exercises ScaleUpScheduler end-to-end against a fast test check-interval (see
 * {@link ScaleUpSchedulerTestProfile}, which re-enables the scheduler that's off
 * by default in tests — see application.properties): record enough queue-depth
 * samples over threshold, then await the engine actually scaling up. Runs against
 * K8sScalingClient's no-op mode (workload.k8s.enabled=false by default), same as
 * EngineFlowTest.
 */
@QuarkusTest
@TestProfile(ScaleUpSchedulerTestProfile.class)
class ScaleUpFlowTest {

    @Test
    void sustainedQueueDepthAboveThreshold_triggersScaleUp() {
        String name = "scaleup_engine_" + UUID.randomUUID();
        String createBody = """
                {"name": "%s", "minReplicas": 0, "maxReplicas": 5, "autoStopEnabled": false,
                 "autoStopIdleMinutes": 15, "k8sNamespace": "ns", "k8sDeploymentName": "dep"}
                """.formatted(name);
        String id = given().contentType(ContentType.JSON).body(createBody)
                .when().post("/api/v1/engines")
                .then().statusCode(201)
                .body("currentReplicas", equalTo(0))
                .extract().path("id");

        // Record several samples, all above the test threshold (see
        // application.properties' %test.workload.scaleup.queue-depth-threshold).
        for (int i = 0; i < 3; i++) {
            given().contentType(ContentType.JSON).body("""
                    {"queuedQueryCount": 20, "runningQueryCount": 2}
                    """)
                    .when().post("/api/v1/engines/" + id + "/queue-depth-samples")
                    .then().statusCode(201);
        }

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                given().when().get("/api/v1/engines/" + id)
                        .then().statusCode(200)
                        .body("currentReplicas", greaterThanOrEqualTo(1)));

        given().when().get("/api/v1/engines/" + id + "/scaling-history")
                .then().statusCode(200)
                .body("find { it.eventType == 'SCALED_UP' }.reason", org.hamcrest.Matchers.containsString("Queue-depth scale-up"));
    }

    @Test
    void queueDepthBelowThreshold_doesNotTriggerScaleUp() {
        String name = "no_scaleup_engine_" + UUID.randomUUID();
        String createBody = """
                {"name": "%s", "minReplicas": 0, "maxReplicas": 5, "autoStopEnabled": false,
                 "autoStopIdleMinutes": 15, "k8sNamespace": "ns", "k8sDeploymentName": "dep"}
                """.formatted(name);
        String id = given().contentType(ContentType.JSON).body(createBody)
                .when().post("/api/v1/engines")
                .then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).body("""
                {"queuedQueryCount": 1, "runningQueryCount": 1}
                """)
                .when().post("/api/v1/engines/" + id + "/queue-depth-samples")
                .then().statusCode(201);

        // Give the scheduler a couple of real cycles to (not) act, then assert
        // the engine is still at zero replicas.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        given().when().get("/api/v1/engines/" + id)
                .then().statusCode(200)
                .body("currentReplicas", equalTo(0));
    }
}
