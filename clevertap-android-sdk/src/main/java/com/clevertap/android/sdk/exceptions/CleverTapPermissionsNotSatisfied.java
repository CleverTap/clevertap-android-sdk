/*
 * Author: Jude Pereira
 * Copyright (c) 2014
 */

package com.clevertap.android.sdk.exceptions;

/**
 * Thrown when the required permissions necessary for the CleverTap SDK to
 * operate correctly are not available.
 */@Deprecated
public final class CleverTapPermissionsNotSatisfied extends CleverTapException {
    /**
     * This creates a new exception when the permissions are not met.
     *
     * @param detailMessage The message describing the specific permission which is not available
     */
    public CleverTapPermissionsNotSatisfied(String detailMessage) {
        super(detailMessage);
    }
}
