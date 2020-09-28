package com.clevertap.android.sdk;

import org.json.JSONObject;

public interface SyncListener {

    void profileDataUpdated(JSONObject updates);

    void profileDidInitialize(String CleverTapID);
}
