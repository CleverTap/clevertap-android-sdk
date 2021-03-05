package com.clevertap.android.sdk.exceptions;

public class FeatureNotSupportedException extends RuntimeException {

    public FeatureNotSupportedException(final String message) {
        super(message);
    }
}