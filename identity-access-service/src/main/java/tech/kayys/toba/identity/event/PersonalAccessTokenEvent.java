package com.dremioplatform.identity.event;

import com.dremioplatform.identity.domain.PrincipalType;
import java.time.Instant;
import java.util.UUID;

public record PersonalAccessTokenEvent(
        UUID eventId,
        Instant occurredAt,
        UUID tokenId,
        PrincipalType principalType,
        UUID principalId,
        Transition transition
) implements IdentityEvent {

    public enum Transition { ISSUED, REVOKED, EXPIRED }

    public static PersonalAccessTokenEvent of(UUID tokenId, PrincipalType principalType, UUID principalId, Transition transition) {
        return new PersonalAccessTokenEvent(UUID.randomUUID(), Instant.now(), tokenId, principalType, principalId, transition);
    }
}
