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
 * Admin-configured external identity provider. Closes the OSS "External LDAP or
 * OpenID Identity Provider" gap. connectionConfig holds provider-specific settings
 * (issuer URL + client id/secret ref for OIDC; host/base-DN/bind-DN ref for LDAP)
 * as JSON — kept schema-flexible on purpose since provider config shapes vary a lot
 * and this table isn't queried by its contents, only loaded whole by id.
 *
 * Secrets (client secret, bind password) are never stored inline — connectionConfig
 * holds a reference key into the platform's Vault/KMS secret store, resolved at
 * runtime by the service layer, not the REST layer, and never serialized back out.
 */
@Entity
@Table(name = "identity_provider_config")
public class IdentityProviderConfig extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public IdentityProviderType type;

    @Column(nullable = false, columnDefinition = "jsonb")
    public String connectionConfigJson;

    public boolean enabled = true;

    /** SCIM base URL for this provider, if it pushes provisioning events to us rather than us pulling. */
    public String scimEndpoint;

    public Instant createdAt = Instant.now();
}
