package com.dremioplatform.identity.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A long-lived, revocable credential owned by a User or ServiceAccount. Only the
 * SHA-256 hash of the token is stored — the raw token is shown exactly once at
 * creation time (returned in the create-PAT response, never persisted or logged).
 * Closes the OSS "Personal Access Tokens" gap.
 */
@Entity
@Table(name = "personal_access_token")
public class PersonalAccessToken extends PanacheEntityBase {

    @Id
    public UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public PrincipalType principalType;

    @Column(nullable = false)
    public UUID principalId;

    @Column(nullable = false)
    public String label;

    @Column(nullable = false, unique = true)
    public String tokenHash;

    /** First 8 chars of the raw token, stored in plaintext for display ("pat_a1b2c3d8...") so users can tell tokens apart without re-hashing. */
    @Column(nullable = false)
    public String displayPrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TokenStatus status = TokenStatus.ACTIVE;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant expiresAt;

    public Instant lastUsedAt;

    public Instant revokedAt;
}
