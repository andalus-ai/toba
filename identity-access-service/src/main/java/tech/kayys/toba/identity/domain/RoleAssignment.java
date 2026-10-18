package com.dremioplatform.identity.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Binds one principal (User or ServiceAccount, via {@link PrincipalType} discriminator
 * rather than a polymorphic FK — keeps the AuthCheck query a single-table lookup) to
 * one Role at one scope.
 *
 * scopePath format mirrors Dremio's namespace addressing, e.g.:
 *   "org"                                  -> RoleScopeType.ORGANIZATION
 *   "space.sales"                          -> RoleScopeType.SPACE_OR_SOURCE
 *   "space.sales.folder.q4"                -> RoleScopeType.FOLDER
 *   "space.sales.folder.q4.dataset.orders" -> RoleScopeType.DATASET
 *
 * The gRPC AuthCheckService (Phase 1 step 5) resolves "most specific scope wins"
 * by querying all assignments whose scopePath is a prefix of the requested path
 * and picking the longest match, then unions the granted permissions.
 */
@Entity
@Table(name = "role_assignment")
public class RoleAssignment extends PanacheEntityBase {

    @Id
    public UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public PrincipalType principalType;

    /** FK to User.id or ServiceAccount.id, depending on principalType. */
    @Column(nullable = false)
    public UUID principalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    public Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RoleScopeType scopeType;

    @Column(nullable = false)
    public String scopePath;

    public UUID grantedByUserId;

    @Column(nullable = false)
    public Instant grantedAt = Instant.now();

    /** Null = no expiry. Time-boxed grants close another common enterprise-IAM gap. */
    public Instant expiresAt;
}
