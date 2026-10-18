package com.dremioplatform.common.error;

/** Thrown for semantically-invalid input that passes bean validation but fails
 *  domain rules (e.g. assigning a role scope that doesn't exist). Maps to HTTP 400. */
public class ValidationException extends PlatformException {

    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public int httpStatus() {
        return 400;
    }
}
