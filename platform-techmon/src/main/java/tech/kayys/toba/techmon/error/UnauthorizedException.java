package com.dremioplatform.common.error;

/** Thrown when credentials (password, PAT, JWT) fail to authenticate. Maps to HTTP 401. */
public class UnauthorizedException extends PlatformException {

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public int httpStatus() {
        return 401;
    }
}
