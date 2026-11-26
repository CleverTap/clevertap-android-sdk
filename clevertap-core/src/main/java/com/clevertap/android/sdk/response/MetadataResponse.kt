package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.IJRepo
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class MetadataResponse(
    private val config: CleverTapInstanceConfig,
    private val deviceInfo: DeviceInfo,
    private val ijRepo: IJRepo
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        // Always look for a GUID in the response, and if present, then perform a force update
        try {
            // A null body is reported here; the i/j sections below then skip it silently.
            val response = checkNotNull(jsonBody) { "response JSON is null" }
            if (response.has("g")) {
                val deviceID = response.getString("g")
                deviceInfo.forceUpdateDeviceId(deviceID)
                logger.verbose(config.accountId, "Got a new device ID: $deviceID")
            }
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "Failed to update device ID!", t)
        }
        val response = jsonBody ?: return

        // Handle i
        try {
            if (response.has("_i")) {
                ijRepo.setI(context, response.getLong("_i"))
            }
        } catch (t: Throwable) {
            // Ignore
        }

        // Handle j
        try {
            if (response.has("_j")) {
                ijRepo.setJ(context, response.getLong("_j"))
            }
        } catch (t: Throwable) {
            // Ignore
        }
    }
}
