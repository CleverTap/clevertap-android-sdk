package com.clevertap.android.sdk.exceptions;

public class FeatureNotSupportedException extends RuntimeException {

    //TODO revisit in code review
    public FeatureNotSupportedException(final String message) {
        super(message);
    }
}