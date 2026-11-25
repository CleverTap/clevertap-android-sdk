package com.clevertap.android.sdk;

import org.json.JSONObject;

public interface GeofenceCallback {

    void handleGeoFences(JSONObject jsonObject);

    void triggerLocation();
}
