package com.clevertap.android.sdk;

import java.util.HashMap;

public interface InAppNotificationButtonListener {

    /**
     * Callback to return a Key Value payload associated with inApp widget click.
     */
    void onInAppButtonClick(HashMap<String, String> payload);
}