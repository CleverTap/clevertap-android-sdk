package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.BaseLocationManager
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.GeofenceCallback
import com.clevertap.android.sdk.response.GeofenceResponse
import org.json.JSONObject

/**
 * Geofence feature
 * Manages geofencing operations and location tracking
 */
internal data class GeofenceFeature(
    val locationManager: BaseLocationManager,
    val geofenceResponse: GeofenceResponse
) : CleverTapFeature {

    lateinit var coreContract: CoreContract
    var geofenceCallback: GeofenceCallback? = null

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(
        response: JSONObject,
        stringBody: String,
        context: Context
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
}
