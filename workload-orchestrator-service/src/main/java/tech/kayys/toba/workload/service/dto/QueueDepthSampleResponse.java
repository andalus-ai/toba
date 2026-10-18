package com.dremioplatform.workload.service.dto;

import com.dremioplatform.workload.domain.QueueDepthSample;
import java.time.Instant;
import java.util.UUID;

public record QueueDepthSampleResponse(UUID id, UUID engineId, int queuedQueryCount, int runningQueryCount, Instant sampledAt) {
    public static QueueDepthSampleResponse from(QueueDepthSample s) {
        return new QueueDepthSampleResponse(s.id, s.engineId, s.queuedQueryCount, s.runningQueryCount, s.sampledAt);
    }
}
