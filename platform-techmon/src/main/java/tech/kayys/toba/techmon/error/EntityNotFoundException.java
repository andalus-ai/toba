package com.dremioplatform.common.error;

import java.util.UUID;

/** Thrown when a lookup by id/natural key finds nothing. Maps to HTTP 404. */
public class EntityNotFoundException extends PlatformException {

    public EntityNotFoundException(String errorCode, String entityType, UUID id) {
        super(errorCode, entityType + " with id '" + id + "' was not found");
    }

    public EntityNotFoundException(String errorCode, String entityType, String naturalKey) {
        super(errorCode, entityType + " '" + naturalKey + "' was not found");
    }

    @Override
    public int httpStatus() {
        return 404;
    }
}
