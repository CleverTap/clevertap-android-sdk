package com.clevertap.android.xps;

/**
 * Throw this exception when registration fails due to invalid configurations
 */
public class RegistrationException extends RuntimeException {

    public RegistrationException(String message) {
        super(message);
    }
}