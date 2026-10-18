package com.dremioplatform.governance.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Row/column masking policy (plan Section 5.2 / 9). Evaluated by the dremio-adapter's
 * AccessControlChecker at plan time: for COLUMN_MASK, the adapter rewrites the
 * logical plan to wrap the target column in a CASE expression ("if principal holds
 * one of exemptRoleIds, real value, else `expression`"); for ROW_FILTER, the adapter
 * adds `expression` as an additional filter predicate unless the principal is exempt.
 *
 * This service owns policy *definitions* only — it never touches a Dremio plan
 * itself; the actual RelNode rewrite happens in the adapter module, which calls
 * `GET /api/v1/masking-policies/applicable?scopePath=...` (or the gRPC equivalent,
 * not yet built) to fetch policies for a query's referenced datasets.
 */
@Entity
@Table(name = "masking_policy")
public class MaskingPolicy extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    /** Dataset (or ancestor scope) this policy applies to — same scope-path addressing as Identity's RoleAssignment. */
    @Column(nullable = false)
    public String scopePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public MaskingRuleType ruleType;

    /** Required for COLUMN_MASK, null for ROW_FILTER. */
    public String columnName;

    /**
     * COLUMN_MASK: the masked replacement expression, e.g. "'***-**-' || RIGHT(ssn, 4)".
     * ROW_FILTER: the predicate rows must satisfy to be visible, e.g. "region = 'US'".
     * Stored as a raw SQL fragment the adapter splices into a Calcite rewrite —
     * validated against a fragment allowlist in the service layer before persist,
     * never executed by this service itself.
     */
    @Column(nullable = false, columnDefinition = "text")
    public String expression;

    /** Role ids (from Identity & Access Service — not enforced by FK, cross-service) exempt from this policy. */
    @ElementCollection
    @CollectionTable(name = "masking_policy_exempt_role", joinColumns = @JoinColumn(name = "masking_policy_id"))
    @Column(name = "role_id")
    public Set<UUID> exemptRoleIds = new HashSet<>();

    public boolean enabled = true;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public UUID createdByUserId;
}
