package com.dremioplatform.workload.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * The shape the not-yet-built job-submission routing hook would report on an
 * interval — see {@link com.dremioplatform.workload.domain.QueueDepthSample}'s
 * Javadoc. In this increment, called directly via REST.
 */
public record RecordQueueDepthSampleRequest(
        @NotNull @Min(0) Integer queuedQueryCount,
        @NotNull @Min(0) Integer runningQueryCount
) {
}
