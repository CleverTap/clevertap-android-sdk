package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.GeofenceCallback
import com.clevertap.android.sdk.response.GeofenceResponse
import org.json.JSONObject

/**
 * Geofence feature
 * Manages geofencing operations and location tracking
 */
internal class GeofenceFeature() : CleverTapFeature {

    lateinit var coreContract: CoreContract
    var geofenceCallback: GeofenceCallback? = null

    // Lazy-initialized Geofence dependencies (initialized after coreContract is set)
    val geofenceResponse: GeofenceResponse by lazy {
        GeofenceResponse(
            coreContract.config().accountId,
            coreContract.logger()
        )
    }

    override fun handleApiData(response: JSONObject) {
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing geofence response"
            )
            return
        }
        geofenceResponse.processResponse(response, geofenceCallback)
    }
}
