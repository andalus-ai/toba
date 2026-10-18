package com.dremioplatform.common.error;

/**
 * Root of the platform's domain exception hierarchy. Every service-layer failure
 * that should be translated into an RFC 7807 problem-detail response extends this
 * class rather than throwing a raw RuntimeException, so the REST layer can map
 * consistently across all microservices (see ProblemDetailMapper in each service).
 */
public abstract class PlatformException extends RuntimeException {

    private final String errorCode;

    protected PlatformException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected PlatformException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Stable, machine-readable code (e.g. "IDENTITY-404-USER-NOT-FOUND") — distinct
     * from the HTTP status, and safe to log/alert on without parsing prose messages.
     */
    public String errorCode() {
        return errorCode;
    }

    /** HTTP status this exception should map to. */
    public abstract int httpStatus();
}
