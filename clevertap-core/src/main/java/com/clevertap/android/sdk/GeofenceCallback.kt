package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GeofenceCallback {

    public fun handleGeoFences(jsonObject: JSONObject?)

    public fun triggerLocation()
}
