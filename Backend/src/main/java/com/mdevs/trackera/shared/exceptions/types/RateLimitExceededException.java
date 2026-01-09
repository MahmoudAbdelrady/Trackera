package com.mdevs.trackera.shared.exceptions.types;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
