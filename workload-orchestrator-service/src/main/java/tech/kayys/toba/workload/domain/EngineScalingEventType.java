package com.dremioplatform.workload.domain;

public enum EngineScalingEventType {
    STARTED,
    STOPPED,
    SCALED_UP,
    SCALED_DOWN,
    AUTO_STOPPED_IDLE
}
