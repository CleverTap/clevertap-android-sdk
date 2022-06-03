package com.clevertap.android.sdk.pushnotification;

import android.content.Context;

/**
 * Defines a {@link CTPushProvider push provider} that has an ability to unregister(stop push service)
 */
public interface UnregistrableCTPushProvider {

    /**
     * Turn off the push service
     */
    void unregisterPush(final Context context);

}
