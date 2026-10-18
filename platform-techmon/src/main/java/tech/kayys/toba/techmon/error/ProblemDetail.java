package com.dremioplatform.common.error;

import java.time.Instant;

/**
 * RFC 7807 "application/problem+json" body. Every REST layer across every
 * microservice returns this shape on error, so API Gateway / UI clients only
 * need one error-parsing code path regardless of which downstream service failed.
 */
public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String errorCode,
        Instant timestamp
) {
    public static ProblemDetail of(PlatformException ex, String instancePath) {
        return new ProblemDetail(
                "about:blank",
                ex.getClass().getSimpleName(),
                ex.httpStatus(),
                ex.getMessage(),
                instancePath,
                ex.errorCode(),
                Instant.now()
        );
    }
}
