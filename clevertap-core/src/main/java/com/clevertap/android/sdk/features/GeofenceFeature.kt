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
    private var geofenceCallback: GeofenceCallback? = null

    // Lazy-initialized Geofence dependencies (initialized after coreContract is set)
    val geofenceResponse: GeofenceResponse by lazy {
        GeofenceResponse(
            coreContract.config().accountId,
            coreContract.logger()
        )
    }

    override fun handleApiData(
        response: JSONObject,
        isFullResponse: Boolean,
        isUserSwitching: Boolean
    ) {
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing geofence response"
            )
            return
        }
        geofenceResponse.processResponse(response, geofenceCallback)
    }

    // ========== PUBLIC API FACADES ==========
    // These methods provide direct delegation from CleverTapAPI to Geofence functionality
    // Signature matches CleverTapAPI public methods for 1:1 mapping

    /**
     * Returns the GeofenceCallback object
     */
    fun getGeofenceCallback(): GeofenceCallback? {
        return geofenceCallback
    }

    /**
     * Sets the geofence callback
     */
    fun setGeofenceCallback(callback: GeofenceCallback?) {
        this.geofenceCallback = callback
    }

    // ========== PUBLIC API FACADES END ==========
}
