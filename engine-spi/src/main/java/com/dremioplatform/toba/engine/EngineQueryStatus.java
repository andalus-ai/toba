package com.dremioplatform.toba.engine;

import java.time.Instant;
import java.util.Objects;

public record EngineQueryStatus(State state, Instant submittedAt, Instant startedAt,
                                Instant finishedAt, String safeMessage) {
    public enum State { QUEUED, PLANNING, RUNNING, SUCCEEDED, FAILED, CANCELLED, UNKNOWN }

    public EngineQueryStatus {
        Objects.requireNonNull(state, "state");
        // safeMessage must be sanitized by the engine adapter; do not include SQL, secrets,
        // connection strings or raw provider response bodies.
        if (safeMessage != null && safeMessage.length() > 512) {
            safeMessage = safeMessage.substring(0, 512);
        }
    }

    public boolean terminal() {
        return state == State.SUCCEEDED || state == State.FAILED || state == State.CANCELLED;
    }
}
