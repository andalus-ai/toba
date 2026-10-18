package com.dremioplatform.workload.scheduler;

import com.dremioplatform.workload.domain.Engine;
import com.dremioplatform.workload.domain.EngineScalingEvent;
import com.dremioplatform.workload.domain.EngineScalingEventType;
import com.dremioplatform.workload.domain.EngineStatus;
import com.dremioplatform.workload.event.WorkloadEvent;
import com.dremioplatform.workload.event.WorkloadEventPublisher;
import com.dremioplatform.workload.k8s.K8sScalingClient;
import com.dremioplatform.workload.repository.EngineRepository;
import com.dremioplatform.workload.repository.EngineScalingEventRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * The "background Quarkus scheduled/reactive job" the plan calls for (Section 5.4:
 * "auto-start/stop ... based on queue depth/queued-query metrics" — this increment
 * implements the idle-timeout half; queue-depth-triggered scale-UP is a separate,
 * not-yet-built piece since it needs live queue-depth metrics this service doesn't
 * collect yet — see module README "Known gaps").
 *
 * Runs every `workload.autostop.check-interval` (default 1 minute — frequent enough
 * that an engine won't sit idle-and-billing for long after crossing its threshold,
 * infrequent enough not to hammer the K8s API). Each qualifying engine is scaled to
 * zero exactly like a manual stop, through the same K8s call + event-recording path,
 * so its scaling history doesn't distinguish "why" beyond the event's reason string
 * and {@code eventType = AUTO_STOPPED_IDLE}.
 */
@ApplicationScoped
public class IdleAutoStopScheduler {

    private static final Logger LOG = Logger.getLogger(IdleAutoStopScheduler.class);

    @Inject EngineRepository engineRepository;
    @Inject EngineScalingEventRepository scalingEventRepository;
    @Inject K8sScalingClient k8sScalingClient;
    @Inject WorkloadEventPublisher eventPublisher;

    @Scheduled(every = "{workload.autostop.check-interval}")
    public Uni<Void> checkIdleEngines() {
        return engineRepository.findRunningWithAutoStopEnabled()
                .chain(engines -> Uni.join().all(
                        engines.stream().map(this::stopIfIdle).toList()
                ).andFailFast())
                .replaceWithVoid()
                .onFailure().invoke(t -> LOG.error("Idle-auto-stop sweep failed", t));
    }

    @WithTransaction
    Uni<Void> stopIfIdle(Engine engine) {
        if (!isIdle(engine)) {
            return Uni.createFrom().voidItem();
        }

        LOG.infof("Auto-stopping idle engine %s (idle > %d min)", engine.name, engine.autoStopIdleMinutes);
        int fromReplicas = engine.currentReplicas;

        return Uni.createFrom().item(() -> {
                    k8sScalingClient.scale(engine.k8sNamespace, engine.k8sDeploymentName, 0);
                    return null;
                })
                .chain(ignored -> {
                    engine.currentReplicas = 0;
                    engine.status = EngineStatus.STOPPED;

                    EngineScalingEvent event = new EngineScalingEvent();
                    event.id = UUID.randomUUID();
                    event.engineId = engine.id;
                    event.eventType = EngineScalingEventType.AUTO_STOPPED_IDLE;
                    event.fromReplicas = fromReplicas;
                    event.toReplicas = 0;
                    event.reason = "Idle for over " + engine.autoStopIdleMinutes + " minutes";

                    return scalingEventRepository.persist(event)
                            .chain(() -> eventPublisher.publish(WorkloadEvent.of(
                                    engine.id, EngineScalingEventType.AUTO_STOPPED_IDLE, fromReplicas, 0,
                                    event.reason, null)))
                            .replaceWithVoid();
                })
                .onFailure().invoke(t -> LOG.errorf(t, "Failed to auto-stop engine %s", engine.name));
    }

    private boolean isIdle(Engine engine) {
        if (engine.lastActivityAt == null) {
            return false; // never activated — nothing to idle out of; avoids stopping a freshly-started engine before its first query lands
        }
        Duration idleFor = Duration.between(engine.lastActivityAt, Instant.now());
        return idleFor.toMinutes() >= engine.autoStopIdleMinutes;
    }
}
