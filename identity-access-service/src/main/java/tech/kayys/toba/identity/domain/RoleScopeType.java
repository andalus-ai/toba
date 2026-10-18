package com.dremioplatform.identity.domain;

/**
 * The granularity a RoleAssignment is anchored to. Mirrors Dremio's own namespace
 * hierarchy (organization > space/source > folder > dataset) so the Governance
 * Service's masking-policy lookups and the dremio-adapter's authz checks can both
 * resolve "most specific scope wins" without the Identity Service knowing anything
 * about Dremio's internal catalog representation — it only stores the scope path.
 */
public enum RoleScopeType {
    ORGANIZATION,
    SPACE_OR_SOURCE,
    FOLDER,
    DATASET
}
