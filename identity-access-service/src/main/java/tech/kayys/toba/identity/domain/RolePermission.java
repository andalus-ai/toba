package com.dremioplatform.identity.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * Explicit join entity (rather than a bare @ManyToMany) so we can attach metadata
 * later (e.g. grantedBy, grantedAt) without a schema-breaking migration.
 */
@Entity
@Table(name = "role_permission", uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}))
public class RolePermission extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    public Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    public Permission permission;
}
