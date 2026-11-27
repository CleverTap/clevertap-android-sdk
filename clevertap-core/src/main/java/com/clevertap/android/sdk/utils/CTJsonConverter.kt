package com.clevertap.android.sdk.utils

import android.os.Build
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.validation.ValidationResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object CTJsonConverter {

    private const val TAG = "CTJsonConverter"

    @JvmStatic
    fun toJsonObject(json: String?, logger: ILogger, logtag: String?): JSONObject {
        if (json == null) {
            return JSONObject()
        }
        return try {
            JSONObject(json)
        } catch (t: Throwable) {
            logger.verbose(logtag, "Error reading guid cache: ", t)
            JSONObject()
        }
    }

    @JvmStatic
    fun displayUnitFromExtras(extras: Bundle): JSONObject {
        val pushJsonPayload = extras.getString(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY)
        Logger.v(TAG, "Received Display Unit via push payload: $pushJsonPayload")
        val displayUnits = JSONArray()
        val r = JSONObject().put(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY, displayUnits)
        displayUnits.put(JSONObject(pushJsonPayload))
        return r
    }

    // Keys from() writes itself. A shared module version with one of these names would replace the real value
    // in af for every instance, so CoreMetaData rejects them. Keep in sync with from().
    val RESERVED_AF_KEYS: Set<String> = setOf(
        "Build", "Version", "OS Version", "SDK Version", "Latitude", "Longitude",
        "GoogleAdID", "${Constants.MULTI_USER_PREFIX}GoogleAdID", "GoogleAdIDLimit",
        "Make", "Model", "Carrier", "useIP", "OS", "wdt", "hgt", "dpi", "dt", "locale", "abckt", "lib",
        "proxyDomain", "spikyProxyDomain", "sslpin", "fcmsid", "cc",
        "wifi", "BluetoothEnabled", "BluetoothVersion", "Radio", "LIAMC"
    )

    @JvmStatic
    fun from(
        deviceInfo: DeviceInfo,
        coreMetaData: CoreMetaData,
        enableNetworkInfoReporting: Boolean,
        deviceIsMultiUser: Boolean
    ): JSONObject {
        val evtData = JSONObject()
        val locationFromUser = coreMetaData.locationFromUser

        evtData.put("Build", "${deviceInfo.build}")
        evtData.put("Version", deviceInfo.versionName)
        evtData.put("OS Version", deviceInfo.osVersion)
        evtData.put("SDK Version", deviceInfo.sdkVersion)

        if (locationFromUser != null) {
            evtData.put("Latitude", locationFromUser.latitude)
            evtData.put("Longitude", locationFromUser.longitude)
        }

        // send up googleAdID
        putGoogleAdId(evtData = evtData, deviceInfo = deviceInfo, deviceIsMultiUser = deviceIsMultiUser)

        try {
            // Device data
            evtData.put("Make", deviceInfo.manufacturer)
            evtData.put("Model", deviceInfo.model)
            evtData.put("Carrier", deviceInfo.carrier)
            evtData.put("useIP", enableNetworkInfoReporting)
            evtData.put("OS", deviceInfo.osName)
            evtData.put("wdt", deviceInfo.width)
            evtData.put("hgt", deviceInfo.height)
            evtData.put("dpi", deviceInfo.dpi)
            evtData.put("dt", DeviceInfo.getDeviceType(deviceInfo.context))
            evtData.put("locale", deviceInfo.locale)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                evtData.put("abckt", deviceInfo.appBucket)
            }
            deviceInfo.library?.let { evtData.put("lib", it) }

            putManifestInfo(evtData = evtData, manifestInfo = ManifestInfo.getInstance(deviceInfo.context))

            val cc = deviceInfo.countryCode
            if (!cc.isNullOrEmpty()) {
                evtData.put("cc", cc)
            }

            if (enableNetworkInfoReporting) {
                putNetworkInfo(evtData = evtData, deviceInfo = deviceInfo)
            }
            //Adds an extra field to send local inApp count in queueData.
            evtData.put("LIAMC", deviceInfo.localInAppCount)

            // add custom sdk versions in "af" key of header.
            for ((key, value) in coreMetaData.allCustomSdkVersions) {
                evtData.put(key, value)
            }
        } catch (t: Throwable) {
            // Ignore
        }

        return evtData
    }

    private fun putGoogleAdId(evtData: JSONObject, deviceInfo: DeviceInfo, deviceIsMultiUser: Boolean) {
        val googleAdID = deviceInfo.googleAdID
        if (googleAdID != null) {
            val baseAdIDKey = "GoogleAdID"
            val adIDKey = if (deviceIsMultiUser) { "${Constants.MULTI_USER_PREFIX}$baseAdIDKey" } else { baseAdIDKey }
            evtData.put(adIDKey, googleAdID)
            evtData.put("GoogleAdIDLimit", deviceInfo.isLimitAdTrackingEnabled)
        }
    }

    private fun putManifestInfo(evtData: JSONObject, manifestInfo: ManifestInfo) {
        val proxyDomain = manifestInfo.proxyDomain
        if (!proxyDomain.isNullOrEmpty()) {
            evtData.put("proxyDomain", proxyDomain)
        }

        val spikyProxyDomain = manifestInfo.spikeyProxyDomain
        if (!spikyProxyDomain.isNullOrEmpty()) {
            evtData.put("spikyProxyDomain", spikyProxyDomain)
        }

        if (manifestInfo.isSSLPinningEnabled) {
            evtData.put("sslpin", true)
        }

        //only for 4.3.0 for tracking custom sender ID users.
        if (!manifestInfo.fcmSenderId.isNullOrEmpty()) {
            evtData.put("fcmsid", true)
        }
    }

    private fun putNetworkInfo(evtData: JSONObject, deviceInfo: DeviceInfo) {
        deviceInfo.isWifiConnected?.let { evtData.put("wifi", it) }
        deviceInfo.isBluetoothEnabled?.let { evtData.put("BluetoothEnabled", it) }
        deviceInfo.bluetoothVersion?.let { evtData.put("BluetoothVersion", it) }
        deviceInfo.networkType?.let { evtData.put("Radio", it) }
    }

    //Validation
    @JvmStatic
    fun getErrorObject(vr: ValidationResult): JSONObject {
        val error = JSONObject()
        try {
            error.put("c", vr.errorCode)
            error.put("d", vr.errorDesc)
        } catch (e: JSONException) {
            // Won't reach here
        }
        return error
    }

    @JvmStatic
    fun pushIdsToJSONArray(pushIds: Array<out String?>): JSONArray {
        val renderedTargets = JSONArray()
        for (pushId in pushIds) {
            Logger.v(TAG, "RTL IDs -$pushId")
            renderedTargets.put(pushId)
        }
        return renderedTargets
    }

    /**
     * The `wzrk_` fields of the in-app's JSON description. A null in-app throws (callers catch it).
     */
    @JvmStatic
    fun getWzrkFields(root: CTInAppNotification?): JSONObject {
        val jsonObject = checkNotNull(root) { "in-app notification is null" }.jsonDescription
        val fields = JSONObject()
        for (keyName in jsonObject.keys()) {
            if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                fields.put(keyName, jsonObject.get(keyName))
            }
        }
        return fields
    }

    @JvmStatic
    fun getWzrkFields(root: CTInboxMessage): JSONObject = root.wzrkParams

    @JvmStatic
    fun <T> toArray(jsonArray: JSONArray): Array<Any?> {
        val array = arrayOfNulls<Any>(jsonArray.length())
        try {
            for (i in array.indices) {
                array[i] = jsonArray.get(i)
            }
        } catch (e: JSONException) {
            Logger.v(TAG, "Error converting JSONArray to array", e)
        }
        return array
    }
}
