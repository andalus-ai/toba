package com.dremioplatform.identity.event;

import java.time.Instant;
import java.util.UUID;

public record UserLifecycleEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        String username,
        Transition transition,
        UUID actorUserId
) implements IdentityEvent {

    public enum Transition { CREATED, SUSPENDED, REACTIVATED, DEPROVISIONED }

    public static UserLifecycleEvent of(UUID userId, String username, Transition transition, UUID actorUserId) {
        return new UserLifecycleEvent(UUID.randomUUID(), Instant.now(), userId, username, transition, actorUserId);
    }
}
