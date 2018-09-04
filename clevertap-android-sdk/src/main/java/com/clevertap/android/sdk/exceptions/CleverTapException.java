package com.clevertap.android.sdk.exceptions;

/**
 * User: Jude Pereira
 * Date: 19/06/2015
 * Time: 14:53
 */
@Deprecated
public abstract class CleverTapException extends Exception {
    @SuppressWarnings("unused")
    public CleverTapException() {
    }

    @SuppressWarnings("WeakerAccess")
    public CleverTapException(String detailMessage) {
        super(detailMessage);
    }

    @SuppressWarnings("unused")
    public CleverTapException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    @SuppressWarnings("unused")
    public CleverTapException(Throwable throwable) {
        super(throwable);
    }
}
