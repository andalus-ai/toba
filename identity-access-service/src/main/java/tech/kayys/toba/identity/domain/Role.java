package com.dremioplatform.identity.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A named, admin-definable bundle of permissions. Closes the OSS "Custom Roles" gap
 * (OSS only has coarse built-in read/write ACLs). A handful of roles are seeded as
 * SYSTEM roles (see V1 migration) mirroring Dremio's built-in behavior; everything
 * else is admin-created.
 */
@Entity
@Table(name = "role")
public class Role extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String name;

    public String description;

    /** System roles (e.g. ORG_ADMIN) cannot be deleted or have permissions removed below their floor. */
    public boolean systemRole = false;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public Set<RolePermission> grantedPermissions = new HashSet<>();

    public Instant createdAt = Instant.now();
}
