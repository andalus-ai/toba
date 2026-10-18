package com.dremioplatform.identity.event;

import com.dremioplatform.identity.domain.PrincipalType;
import java.time.Instant;
import java.util.UUID;

public record RoleAssignmentChangedEvent(
        UUID eventId,
        Instant occurredAt,
        ChangeType changeType,
        PrincipalType principalType,
        UUID principalId,
        UUID roleId,
        String scopePath,
        UUID actorUserId
) implements IdentityEvent {

    public enum ChangeType { GRANTED, REVOKED }

    public static RoleAssignmentChangedEvent granted(PrincipalType principalType, UUID principalId,
                                                       UUID roleId, String scopePath, UUID actorUserId) {
        return new RoleAssignmentChangedEvent(
                UUID.randomUUID(), Instant.now(), ChangeType.GRANTED,
                principalType, principalId, roleId, scopePath, actorUserId);
    }

    public static RoleAssignmentChangedEvent revoked(PrincipalType principalType, UUID principalId,
                                                       UUID roleId, String scopePath, UUID actorUserId) {
        return new RoleAssignmentChangedEvent(
                UUID.randomUUID(), Instant.now(), ChangeType.REVOKED,
                principalType, principalId, roleId, scopePath, actorUserId);
    }
}
