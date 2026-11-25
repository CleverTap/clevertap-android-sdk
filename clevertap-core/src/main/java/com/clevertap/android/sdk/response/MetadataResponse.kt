package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.IJRepo
import org.json.JSONObject

internal open class MetadataResponse(
    private val config: CleverTapInstanceConfig,
    private val deviceInfo: DeviceInfo,
    private val ijRepo: IJRepo
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        val response = jsonBody
        // Always look for a GUID in the response, and if present, then perform a force update
        try {
            if (response!!.has("g")) {
                val deviceID = response.getString("g")
                deviceInfo.forceUpdateDeviceId(deviceID)
                logger.verbose(config.accountId, "Got a new device ID: $deviceID")
            }
        } catch (t: Throwable) {
            logger.verbose(config.accountId, "Failed to update device ID!", t)
        }

        // Handle i
        try {
            if (response!!.has("_i")) {
                val i = response.getLong("_i")
                ijRepo.setI(context, i)
            }
        } catch (t: Throwable) {
            // Ignore
        }

        // Handle j
        try {
            if (response!!.has("_j")) {
                val j = response.getLong("_j")
                ijRepo.setJ(context, j)
            }
        } catch (t: Throwable) {
            // Ignore
        }
    }
}
