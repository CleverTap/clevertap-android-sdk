package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface GeofenceCallback {

    fun handleGeoFences(jsonObject: JSONObject?)

    fun triggerLocation()
}
