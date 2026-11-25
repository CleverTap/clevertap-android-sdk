package com.clevertap.android.sdk.utils

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
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
import java.util.Collections

internal object CTJsonConverter {

    private const val TAG = "CTJsonConverter"

    @JvmStatic
    fun toJsonObject(json: String?, logger: ILogger?, logtag: String?): JSONObject {
        var cache: JSONObject? = null
        if (json != null) {
            try {
                cache = JSONObject(json)
            } catch (t: Throwable) {
                // no-op
                logger!!.verbose(logtag, "Error reading guid cache: ", t)
            }
        }

        return cache ?: JSONObject()
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun displayUnitFromExtras(extras: Bundle): JSONObject {
        val r = JSONObject()

        val pushJsonPayload = extras.getString(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY)
        Logger.v(TAG, "Received Display Unit via push payload: $pushJsonPayload")
        val displayUnits = JSONArray()
        r.put(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY, displayUnits)
        val testPushObject = JSONObject(pushJsonPayload)
        displayUnits.put(testPushObject)

        return r
    }

    // Keys from() writes itself. A shared module version with one of these names would replace the real value
    // in af for every instance, so CoreMetaData rejects them. Keep in sync with from().
    @JvmField
    val RESERVED_AF_KEYS: Set<String> = Collections.unmodifiableSet(
        HashSet(
            listOf(
                "Build", "Version", "OS Version", "SDK Version", "Latitude", "Longitude",
                "GoogleAdID", Constants.MULTI_USER_PREFIX + "GoogleAdID", "GoogleAdIDLimit",
                "Make", "Model", "Carrier", "useIP", "OS", "wdt", "hgt", "dpi", "dt", "locale", "abckt", "lib",
                "proxyDomain", "spikyProxyDomain", "sslpin", "fcmsid", "cc",
                "wifi", "BluetoothEnabled", "BluetoothVersion", "Radio", "LIAMC"
            )
        )
    )

    @JvmStatic
    @Throws(JSONException::class)
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
        if (deviceInfo.googleAdID != null) {
            val baseAdIDKey = "GoogleAdID"
            val adIDKey = if (deviceIsMultiUser) Constants.MULTI_USER_PREFIX + baseAdIDKey else baseAdIDKey
            evtData.put(adIDKey, deviceInfo.googleAdID)
            evtData.put("GoogleAdIDLimit", deviceInfo.isLimitAdTrackingEnabled)
        }

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
            if (deviceInfo.library != null) {
                evtData.put("lib", deviceInfo.library)
            }

            val proxyDomain = ManifestInfo.getInstance(deviceInfo.context).proxyDomain
            if (!TextUtils.isEmpty(proxyDomain)) {
                evtData.put("proxyDomain", proxyDomain)
            }

            val spikyProxyDomain = ManifestInfo.getInstance(deviceInfo.context).spikeyProxyDomain
            if (!TextUtils.isEmpty(spikyProxyDomain)) {
                evtData.put("spikyProxyDomain", spikyProxyDomain)
            }

            val sslPinning = ManifestInfo.getInstance(deviceInfo.context).isSSLPinningEnabled
            if (sslPinning) {
                evtData.put("sslpin", true)
            }

            val fcmSenderId = ManifestInfo.getInstance(deviceInfo.context).fcmSenderId
            if (!TextUtils.isEmpty(fcmSenderId)) { //only for 4.3.0 for tracking custom sender ID users.
                evtData.put("fcmsid", true)
            }

            val cc = deviceInfo.countryCode
            if (cc != null && cc != "") {
                evtData.put("cc", cc)
            }

            if (enableNetworkInfoReporting) {
                val isWifi = deviceInfo.isWifiConnected
                if (isWifi != null) {
                    evtData.put("wifi", isWifi)
                }

                val isBluetoothEnabled = deviceInfo.isBluetoothEnabled
                if (isBluetoothEnabled != null) {
                    evtData.put("BluetoothEnabled", isBluetoothEnabled)
                }

                val bluetoothVersion = deviceInfo.bluetoothVersion
                if (bluetoothVersion != null) {
                    evtData.put("BluetoothVersion", bluetoothVersion)
                }

                val radio = deviceInfo.networkType
                if (radio != null) {
                    evtData.put("Radio", radio)
                }
            }
            //Adds an extra field to send local inApp count in queueData.
            evtData.put("LIAMC", deviceInfo.localInAppCount)

            // add custom sdk versions in "af" key of header.
            val allCustomSdkVersions: Map<String, Int> = coreMetaData.allCustomSdkVersions
            for ((key, value) in allCustomSdkVersions) {
                evtData.put(key, value)
            }
        } catch (t: Throwable) {
            // Ignore
        }

        return evtData
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

    @JvmStatic
    @Throws(JSONException::class)
    fun getWzrkFields(root: CTInAppNotification): JSONObject {
        val fields = JSONObject()
        val jsonObject = root.jsonDescription
        val iterator = jsonObject.keys()

        while (iterator.hasNext()) {
            val keyName = iterator.next()
            if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                fields.put(keyName, jsonObject.get(keyName))
            }
        }

        return fields
    }

    @JvmStatic
    fun getWzrkFields(root: CTInboxMessage): JSONObject? {
        return root.wzrkParams
    }

    @JvmStatic
    fun <T> toArray(jsonArray: JSONArray): Array<Any?> {
        val array = arrayOfNulls<Any>(jsonArray.length())
        try {
            for (i in 0 until jsonArray.length()) {
                array[i] = jsonArray.get(i)
            }
        } catch (e: JSONException) {
            Logger.v(TAG, "Error converting JSONArray to array", e)
        }
        return array
    }

    @JvmStatic
    fun toJsonArray(list: List<*>): JSONArray {
        val array = JSONArray()
        for (item in list) {
            if (item != null) {
                array.put(item)
            }
        }
        return array
    }

    @JvmStatic
    fun toJsonString(value: Any?): String? {
        var `val`: String? = null

        try {
            `val` = value!!.toString()
        } catch (e: Exception) {
            // no-op
        }

        return `val`
    }

    @JvmStatic
    fun toList(array: JSONArray): ArrayList<*> {
        val list = ArrayList<Any?>()
        for (i in 0 until array.length()) {
            try {
                list.add(array.get(i))
            } catch (e: JSONException) {
                Logger.v(TAG, "Error converting JSONArray to list", e)
            }
        }
        return list
    }
}
