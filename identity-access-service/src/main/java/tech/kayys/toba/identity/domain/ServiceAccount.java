package com.dremioplatform.identity.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A non-human principal (CI pipeline, BI tool, another microservice) that
 * authenticates via a PersonalAccessToken or client-credentials OIDC grant
 * rather than an interactive login. Closes the OSS "Service Users" gap.
 */
@Entity
@Table(name = "service_account")
public class ServiceAccount extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String name;

    public String description;

    /** User id of the human who created/owns this service account, for audit. */
    public UUID ownerUserId;

    public boolean enabled = true;

    public Instant createdAt = Instant.now();
}
