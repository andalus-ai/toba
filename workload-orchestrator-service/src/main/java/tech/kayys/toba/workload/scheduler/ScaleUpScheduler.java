package com.dremioplatform.workload.scheduler;

import com.dremioplatform.workload.domain.Engine;
import com.dremioplatform.workload.domain.EngineScalingEvent;
import com.dremioplatform.workload.domain.QueueDepthSample;
import com.dremioplatform.workload.repository.EngineRepository;
import com.dremioplatform.workload.repository.EngineScalingEventRepository;
import com.dremioplatform.workload.repository.QueueDepthSampleRepository;
import com.dremioplatform.workload.service.EngineService;
import com.dremioplatform.workload.service.dto.ScaleEngineRequest;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The queue-depth-triggered scale-UP half of the plan's "auto-start/stop ...
 * based on queue depth/queued-query metrics" (Section 5.4) — the piece
 * {@link IdleAutoStopScheduler} (idle-timeout scale-DOWN) never covered. Evaluates
 * every engine with a recent {@link QueueDepthSample} on each run: if the average
 * queued-query count over the evaluation window exceeds the configured threshold,
 * and the engine isn't still cooling down from a previous scale-up, and it has
 * headroom under {@code maxReplicas}, scale up by {@code workload.scaleup.increment}.
 *
 * Deliberately reuses {@link EngineService#scale}, not a separate scaling code
 * path — the same K8s-call-then-record-then-publish sequence, the same rollback-
 * on-K8s-failure behavior, applies whether a human clicked "scale" in the REST API
 * or this scheduler decided to. `actorPrincipalId` is null for scheduler-triggered
 * scales, distinguishing them in the audit trail from human-triggered ones.
 *
 * Cooldown (`workload.scaleup.cooldown`) exists to prevent flapping: without it, a
 * queue that's still catching up right after a scale-up (new pods take time to
 * become ready) could trigger another scale-up before the first one had a chance
 * to help, ratcheting replicas up faster than actually needed.
 */
@ApplicationScoped
public class ScaleUpScheduler {

    private static final Logger LOG = Logger.getLogger(ScaleUpScheduler.class);

    @Inject QueueDepthSampleRepository queueDepthSampleRepository;
    @Inject EngineRepository engineRepository;
    @Inject EngineScalingEventRepository scalingEventRepository;
    @Inject EngineService engineService;

    @ConfigProperty(name = "workload.scaleup.evaluation-window")
    Duration evaluationWindow;

    @ConfigProperty(name = "workload.scaleup.queue-depth-threshold")
    int queueDepthThreshold;

    @ConfigProperty(name = "workload.scaleup.increment")
    int scaleUpIncrement;

    @ConfigProperty(name = "workload.scaleup.cooldown")
    Duration cooldown;

    @ConfigProperty(name = "workload.scaleup.sample-retention")
    Duration sampleRetention;

    @Scheduled(every = "{workload.scaleup.check-interval}")
    public Uni<Void> evaluate() {
        Instant windowStart = Instant.now().minus(evaluationWindow);

        return queueDepthSampleRepository.findDistinctEngineIdsWithRecentSamples(windowStart)
                .chain(engineIds -> Uni.join().all(
                        engineIds.stream().map(id -> evaluateEngine(id, windowStart)).toList()
                ).andFailFast())
                .chain(ignored -> pruneOldSamples())
                .replaceWithVoid()
                .onFailure().invoke(t -> LOG.error("Scale-up evaluation sweep failed", t));
    }

    @WithTransaction
    Uni<Void> evaluateEngine(UUID engineId, Instant windowStart) {
        return queueDepthSampleRepository.findRecentForEngine(engineId, windowStart)
                .chain(samples -> {
                    if (samples.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    double avgQueued = samples.stream().mapToInt(s -> s.queuedQueryCount).average().orElse(0);
                    if (avgQueued <= queueDepthThreshold) {
                        return Uni.createFrom().voidItem();
                    }
                    return engineRepository.findById(engineId).chain(engine -> {
                        if (engine == null || engine.currentReplicas >= engine.maxReplicas) {
                            return Uni.createFrom().voidItem();
                        }
                        return checkCooldownThenScale(engine, avgQueued, samples.size());
                    });
                });
    }

    private Uni<Void> checkCooldownThenScale(Engine engine, double avgQueued, int sampleCount) {
        return scalingEventRepository.findMostRecentUpwardEvent(engine.id).chain(lastUpwardEvent -> {
            if (lastUpwardEvent != null && withinCooldown(lastUpwardEvent)) {
                LOG.debugf("Engine %s exceeds queue-depth threshold but is within cooldown; skipping", engine.name);
                return Uni.createFrom().voidItem();
            }

            int target = Math.min(engine.maxReplicas, engine.currentReplicas + scaleUpIncrement);
            String reason = String.format(
                    "Queue-depth scale-up: avg queued=%.1f over %d sample(s) exceeds threshold=%d",
                    avgQueued, sampleCount, queueDepthThreshold);

            LOG.infof("Scaling up engine %s: %d -> %d replicas (%s)", engine.name, engine.currentReplicas, target, reason);

            return engineService.scale(engine.id, new ScaleEngineRequest(target, reason), null)
                    .replaceWithVoid()
                    .onFailure().invoke(t -> LOG.errorf(t, "Failed to scale up engine %s", engine.name));
        });
    }

    private boolean withinCooldown(EngineScalingEvent lastUpwardEvent) {
        return Duration.between(lastUpwardEvent.occurredAt, Instant.now()).compareTo(cooldown) < 0;
    }

    @WithTransaction
    Uni<Void> pruneOldSamples() {
        Instant cutoff = Instant.now().minus(sampleRetention);
        return queueDepthSampleRepository.deleteOlderThan(cutoff).replaceWithVoid();
    }
}
