package com.dremioplatform.workload.event;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

/** Same publish-after-commit / idempotent-consumer contract as identity-access-service's
 *  EventPublisher — see that class's Javadoc. */
@ApplicationScoped
public class WorkloadEventPublisher {

    private static final Logger LOG = Logger.getLogger(WorkloadEventPublisher.class);

    @Channel("workload-events")
    MutinyEmitter<WorkloadEvent> emitter;

    public Uni<Void> publish(WorkloadEvent event) {
        return emitter.send(event)
                .onFailure().invoke(t -> LOG.errorf(t, "Failed to publish workload event eventId=%s", event.eventId()));
    }
}
