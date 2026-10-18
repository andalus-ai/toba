package com.dremioplatform.catalog.event;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

/** Mirrors identity-access-service's EventPublisher pattern — see that class's
 *  Javadoc for the publish-after-commit / idempotent-consumer contract. */
@ApplicationScoped
public class CatalogEventPublisher {

    private static final Logger LOG = Logger.getLogger(CatalogEventPublisher.class);

    @Channel("catalog-events")
    MutinyEmitter<CatalogTableEvent> emitter;

    public Uni<Void> publish(CatalogTableEvent event) {
        return emitter.send(event)
                .onFailure().invoke(t -> LOG.errorf(t, "Failed to publish catalog event eventId=%s", event.eventId()));
    }
}
