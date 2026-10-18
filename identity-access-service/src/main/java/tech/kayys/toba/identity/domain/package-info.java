/**
 * Domain model for the Identity &amp; Access Service (implementation-plan Section 5.1).
 *
 * Entities are Hibernate Reactive Panache active-record style ({@code extends
 * PanacheEntityBase} with a manually-assigned {@code UUID id}, rather than the
 * auto-increment {@code PanacheEntity} base) so ids are generated application-side
 * before insert — this lets the service layer reference a new entity's id (e.g. to
 * publish an "IdentityCreated" event) without a round trip.
 *
 * No entity in this package talks to another microservice directly; cross-service
 * effects (e.g. "role assignment changed, invalidate the Governance Service's authz
 * cache") are the service layer's job, published as domain events — see the
 * (forthcoming) {@code com.dremioplatform.identity.event} package in the service-layer
 * increment.
 */
package com.dremioplatform.identity.domain;
