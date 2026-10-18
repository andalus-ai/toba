package com.dremioplatform.identity.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A single grantable capability, e.g. "catalog.dataset.read", "catalog.dataset.write",
 * "governance.masking-policy.manage", "workload.engine.scale". Deliberately a flat,
 * seeded reference table (not an enum) so new services can register new permission
 * strings via migration without a code change/redeploy of this service.
 */
@Entity
@Table(name = "permission")
public class Permission extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, unique = true)
    public String key;

    /** Dot-prefix namespace, e.g. "catalog", "governance", "workload", "identity" — used for grouped admin UI display. */
    @Column(nullable = false)
    public String domain;

    public String description;
}
