package com.dremioplatform.workload.service;

import com.dremioplatform.common.error.ConflictException;
import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.common.error.ValidationException;
import com.dremioplatform.workload.domain.Engine;
import com.dremioplatform.workload.domain.EngineScalingEvent;
import com.dremioplatform.workload.domain.EngineScalingEventType;
import com.dremioplatform.workload.domain.EngineStatus;
import com.dremioplatform.workload.event.WorkloadEvent;
import com.dremioplatform.workload.event.WorkloadEventPublisher;
import com.dremioplatform.workload.k8s.K8sScalingClient;
import com.dremioplatform.workload.repository.EngineRepository;
import com.dremioplatform.workload.repository.EngineScalingEventRepository;
import com.dremioplatform.workload.service.dto.CreateEngineRequest;
import com.dremioplatform.workload.service.dto.EngineResponse;
import com.dremioplatform.workload.service.dto.EngineScalingEventResponse;
import com.dremioplatform.workload.service.dto.ScaleEngineRequest;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EngineService {

    @Inject EngineRepository engineRepository;
    @Inject EngineScalingEventRepository scalingEventRepository;
    @Inject K8sScalingClient k8sScalingClient;
    @Inject WorkloadEventPublisher eventPublisher;

    @WithTransaction
    public Uni<EngineResponse> create(CreateEngineRequest request) {
        if (request.minReplicas() > request.maxReplicas()) {
            return Uni.createFrom().failure(new ValidationException(
                    "WORKLOAD-400-INVALID-REPLICA-RANGE", "minReplicas cannot exceed maxReplicas"));
        }
        return engineRepository.existsByName(request.name())
                .chain(exists -> {
                    if (exists) {
                        return Uni.createFrom().failure(new ConflictException(
                                "WORKLOAD-409-ENGINE-EXISTS", "An engine named '" + request.name() + "' already exists"));
                    }
                    Engine engine = new Engine();
                    engine.id = UUID.randomUUID();
                    engine.name = request.name();
                    engine.description = request.description();
                    engine.minReplicas = request.minReplicas();
                    engine.maxReplicas = request.maxReplicas();
                    engine.currentReplicas = 0;
                    engine.status = EngineStatus.STOPPED;
                    engine.autoStopEnabled = request.autoStopEnabled();
                    engine.autoStopIdleMinutes = request.autoStopIdleMinutes();
                    engine.k8sNamespace = request.k8sNamespace();
                    engine.k8sDeploymentName = request.k8sDeploymentName();
                    return engineRepository.persist(engine).map(EngineResponse::from);
                });
    }

    /** Scales to `max(minReplicas, 1)` — starting an engine with minReplicas=0
     *  still needs at least one replica to actually run anything. */
    @WithTransaction
    public Uni<EngineResponse> start(UUID engineId, UUID actorPrincipalId) {
        return withEngine(engineId, engine -> {
            int target = Math.max(engine.minReplicas, 1);
            return doScale(engine, target, EngineScalingEventType.STARTED, "Manual start", actorPrincipalId,
                    EngineStatus.STARTING, EngineStatus.RUNNING);
        });
    }

    @WithTransaction
    public Uni<EngineResponse> stop(UUID engineId, UUID actorPrincipalId) {
        return withEngine(engineId, engine ->
                doScale(engine, 0, EngineScalingEventType.STOPPED, "Manual stop", actorPrincipalId,
                        EngineStatus.STOPPING, EngineStatus.STOPPED));
    }

    @WithTransaction
    public Uni<EngineResponse> scale(UUID engineId, ScaleEngineRequest request, UUID actorPrincipalId) {
        return withEngine(engineId, engine -> {
            if (request.targetReplicas() > engine.maxReplicas) {
                return Uni.createFrom().failure(new ValidationException(
                        "WORKLOAD-400-EXCEEDS-MAX-REPLICAS",
                        "Requested " + request.targetReplicas() + " exceeds maxReplicas=" + engine.maxReplicas));
            }
            EngineScalingEventType eventType = request.targetReplicas() > engine.currentReplicas
                    ? EngineScalingEventType.SCALED_UP : EngineScalingEventType.SCALED_DOWN;
            EngineStatus resultStatus = request.targetReplicas() == 0 ? EngineStatus.STOPPED : EngineStatus.RUNNING;
            return doScale(engine, request.targetReplicas(), eventType,
                    request.reason() == null ? "Manual scale" : request.reason(),
                    actorPrincipalId, EngineStatus.SCALING, resultStatus);
        });
    }

    /** Called by the not-yet-built job-submission routing hook every time a query
     *  is routed to this engine — resets the idle-auto-stop clock. Exposed as its
     *  own method (not folded into scale/start) since it's a lightweight touch, not
     *  a state transition. */
    @WithTransaction
    public Uni<Void> recordActivity(UUID engineId) {
        return engineRepository.findById(engineId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "WORKLOAD-404-ENGINE-NOT-FOUND", "Engine", engineId))
                .invoke(engine -> engine.lastActivityAt = Instant.now())
                .replaceWithVoid();
    }

    public Uni<EngineResponse> getById(UUID engineId) {
        return engineRepository.findById(engineId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "WORKLOAD-404-ENGINE-NOT-FOUND", "Engine", engineId))
                .map(EngineResponse::from);
    }

    public Uni<List<EngineResponse>> listAll() {
        return engineRepository.listAll().map(list -> list.stream().map(EngineResponse::from).toList());
    }

    public Uni<List<EngineScalingEventResponse>> scalingHistory(UUID engineId, int limit) {
        return scalingEventRepository.findByEngine(engineId, limit)
                .map(list -> list.stream().map(EngineScalingEventResponse::from).toList());
    }

    private Uni<EngineResponse> withEngine(UUID engineId, java.util.function.Function<Engine, Uni<EngineResponse>> fn) {
        return engineRepository.findById(engineId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "WORKLOAD-404-ENGINE-NOT-FOUND", "Engine", engineId))
                .chain(fn);
    }

    /**
     * The shared scale-and-record core: calls K8s, records the scaling event, and
     * publishes the domain event. If the K8s call throws, the whole Uni fails and
     * the transaction rolls back — the Engine row's status/replica count only
     * changes if the underlying infrastructure change actually succeeded, avoiding
     * a "database says running, cluster says stopped" split-brain.
     */
    private Uni<EngineResponse> doScale(Engine engine, int targetReplicas, EngineScalingEventType eventType,
                                          String reason, UUID actorPrincipalId,
                                          EngineStatus transientStatus, EngineStatus resultStatus) {
        int fromReplicas = engine.currentReplicas;
        engine.status = transientStatus;

        return Uni.createFrom().item(() -> {
                    k8sScalingClient.scale(engine.k8sNamespace, engine.k8sDeploymentName, targetReplicas);
                    return null;
                })
                .chain(ignored -> {
                    engine.currentReplicas = targetReplicas;
                    engine.status = resultStatus;
                    if (targetReplicas > 0) {
                        engine.lastActivityAt = Instant.now();
                    }

                    EngineScalingEvent event = new EngineScalingEvent();
                    event.id = UUID.randomUUID();
                    event.engineId = engine.id;
                    event.eventType = eventType;
                    event.fromReplicas = fromReplicas;
                    event.toReplicas = targetReplicas;
                    event.reason = reason;
                    event.actorPrincipalId = actorPrincipalId;

                    return scalingEventRepository.persist(event)
                            .chain(() -> eventPublisher.publish(WorkloadEvent.of(
                                    engine.id, eventType, fromReplicas, targetReplicas, reason, actorPrincipalId)))
                            .map(v -> EngineResponse.from(engine));
                });
    }
}
