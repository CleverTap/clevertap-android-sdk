/*
 * Author: Jude Pereira
 * Copyright (c) 2014
 */

package com.clevertap.android.sdk.exceptions;

/**
 * Thrown when the CleverTap SDK mandatory meta data values are not
 * present in the Android manifest.
 */
@Deprecated
public final class CleverTapMetaDataNotFoundException extends CleverTapException {
    /**
     * This creates a new exception when mandatory meta data fields are not found.
     *
     * @param detailMessage The message describing the cause
     */
    public CleverTapMetaDataNotFoundException(String detailMessage) {
        super(detailMessage);
    }
}
