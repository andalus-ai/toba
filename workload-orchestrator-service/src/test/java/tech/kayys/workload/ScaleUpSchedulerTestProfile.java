package com.dremioplatform.workload;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/**
 * Overrides the default test profile (which disables the scheduler entirely, so
 * fixed-state tests like EngineFlowTest aren't disturbed) specifically for
 * {@link ScaleUpFlowTest}: turns the scheduler back on with a fast check interval
 * so the test doesn't need to wait a full 30s+ for a real scale-up decision, and
 * pushes idle-autostop's interval out so it can't interfere mid-test.
 */
public class ScaleUpSchedulerTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.scheduler.enabled", "true",
                "workload.scaleup.check-interval", "PT2S",
                "workload.scaleup.evaluation-window", "PT30S",
                "workload.scaleup.queue-depth-threshold", "5",
                "workload.scaleup.cooldown", "PT1M",
                "workload.autostop.check-interval", "PT1H"
        );
    }
}
