package com.dremioplatform.identity.domain;

/**
 * Every grantable/token-bearing identity in the system is one of these two kinds.
 * Kept as a discriminator on RoleAssignment/PersonalAccessToken rather than a class
 * hierarchy so a single role-assignment table can serve both without a join per type.
 */
public enum PrincipalType {
    USER,
    SERVICE_ACCOUNT
}
