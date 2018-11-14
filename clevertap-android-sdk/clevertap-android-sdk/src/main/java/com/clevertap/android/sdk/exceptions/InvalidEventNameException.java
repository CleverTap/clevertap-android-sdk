/*
 * Author: Jude Pereira
 * Copyright (c) 2014
 */

package com.clevertap.android.sdk.exceptions;

/**
 * Thrown when an invalid event is handed over to the CleverTap SDK,
 * when the SDK was expecting a particular event.
 */
@Deprecated
public final class InvalidEventNameException extends CleverTapException {
    /**
     * This creates a new exception for an invalid item.
     *
     * @param detailMessage The message describing the cause
     */
    public InvalidEventNameException(String detailMessage) {
        super(detailMessage);
    }
}
