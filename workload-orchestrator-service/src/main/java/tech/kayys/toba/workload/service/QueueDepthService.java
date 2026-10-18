package com.dremioplatform.workload.service;

import com.dremioplatform.common.error.EntityNotFoundException;
import com.dremioplatform.workload.domain.QueueDepthSample;
import com.dremioplatform.workload.repository.EngineRepository;
import com.dremioplatform.workload.repository.QueueDepthSampleRepository;
import com.dremioplatform.workload.service.dto.QueueDepthSampleResponse;
import com.dremioplatform.workload.service.dto.RecordQueueDepthSampleRequest;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class QueueDepthService {

    @Inject QueueDepthSampleRepository queueDepthSampleRepository;
    @Inject EngineRepository engineRepository;

    @WithTransaction
    public Uni<QueueDepthSampleResponse> recordSample(UUID engineId, RecordQueueDepthSampleRequest request) {
        return engineRepository.findById(engineId)
                .onItem().ifNull().failWith(() -> new EntityNotFoundException(
                        "WORKLOAD-404-ENGINE-NOT-FOUND", "Engine", engineId))
                .chain(engine -> {
                    QueueDepthSample sample = new QueueDepthSample();
                    sample.id = UUID.randomUUID();
                    sample.engineId = engineId;
                    sample.queuedQueryCount = request.queuedQueryCount();
                    sample.runningQueryCount = request.runningQueryCount();
                    return queueDepthSampleRepository.persist(sample);
                })
                .map(QueueDepthSampleResponse::from);
    }
}
