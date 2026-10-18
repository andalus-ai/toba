package com.dremioplatform.identity.event;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

/**
 * Every service-layer method that mutates state publishes exactly one event here
 * after (not before) its database write commits successfully — see the
 * `withTransaction(...).call(() -> eventPublisher.publish(...))` pattern in the
 * service layer. Kafka delivery is best-effort/at-least-once: consumers (Governance
 * audit ingestion, cache invalidation) must be idempotent on eventId.
 */
@ApplicationScoped
public class EventPublisher {

    private static final Logger LOG = Logger.getLogger(EventPublisher.class);

    @Channel("identity-events")
    MutinyEmitter<IdentityEvent> emitter;

    public Uni<Void> publish(IdentityEvent event) {
        return emitter.send(event)
                .onFailure().invoke(t -> LOG.errorf(t, "Failed to publish %s eventId=%s",
                        event.getClass().getSimpleName(), event.eventId()));
    }
}
