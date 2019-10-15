package com.clevertap.android.sdk;

import java.util.HashMap;

public interface InAppButtonListener {

    /**
     * Callback to return a Key Value payload associated with inApp widget click.
     *
     * @param payload
     */
    void onClick(HashMap<String, String> payload);
}