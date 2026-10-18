package com.dremioplatform.identity.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * A human identity. Either purely local (username/password against this service)
 * or federated (backed by an external OIDC/SAML/LDAP provider, identified by
 * {@link #externalSubject} + {@link #identityProviderId}).
 *
 * Deliberately does NOT store role assignments inline — see {@link RoleAssignment},
 * a separate table keyed by (principalType, principalId, scope) so both users and
 * service accounts share one assignment/lookup path.
 */
@Entity
@Table(name = "app_user")
public class User extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false, unique = true)
    public String email;

    public String displayName;

    /** Null for purely local users. */
    public UUID identityProviderId;

    /** The provider's "sub" claim (OIDC) or DN (LDAP). Null for local users. */
    public String externalSubject;

    /** BCrypt hash. Null for federated-only users who never set a local password. */
    public String localPasswordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant lastLoginAt;

    /** Optimistic locking — status/role changes race with login-timestamp updates. */
    @Version
    public long version;

    public enum UserStatus {
        ACTIVE,
        SUSPENDED,
        DEPROVISIONED
    }
}
