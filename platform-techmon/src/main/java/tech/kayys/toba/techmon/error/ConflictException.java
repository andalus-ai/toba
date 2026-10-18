package com.dremioplatform.common.error;

/**
 * Thrown for state conflicts: uniqueness violations (e.g. duplicate role name),
 * invalid state transitions (e.g. revoking an already-revoked token), or
 * optimistic-locking version mismatches. Maps to HTTP 409.
 */
public class ConflictException extends PlatformException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }

    @Override
    public int httpStatus() {
        return 409;
    }
}
