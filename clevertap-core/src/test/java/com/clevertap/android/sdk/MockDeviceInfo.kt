package com.clevertap.android.sdk

import android.content.Context
import org.json.JSONObject

class MockDeviceInfo(
    context: Context?, config: CleverTapInstanceConfig?, cleverTapID: String?,
    coreMetaData: CoreMetaData?
) : DeviceInfo(
    context!!, config!!,
    cleverTapID, coreMetaData!!, null
) {

    val ctId: String?

    init {
        ctId = cleverTapID
    }

    override val deviceID: String?
        get() = ctId

    override fun cacheDeviceInfoAsync() {
    }

    override val attributionID: String
        get() = "some-att-id"

    override val bluetoothVersion: String
        get() = "2.0"

    override val build: Int
        get() = 1

    override val carrier: String
        get() = "Android"

    override fun getContext(): Context {
        return super.getContext()
    }

    override val countryCode: String
        get() = "us"

    override fun getDPI(): Int {
        return 420
    }

    override val googleAdID: String
        get() = "__48703e3cc2ff468ab3c641edf2770d74"

    override val height: Double
        get() = 4.27

    override var library: String?
        get() = "Android"
        set(value) {}

    override val manufacturer: String?
        get() = super.manufacturer

    override val model: String
        get() = "sdk_gphone_x86"

    override val networkType: String
        get() = "4G"

    override val osName: String
        get() = "Android"

    override val osVersion: String
        get() = "11"

    override val sdkVersion: Int
        get() = 40100

    override val versionName: String
        get() = "1.0"

    override val width: Double
        get() = 2.57

    override val isBluetoothEnabled: Boolean
        get() = true

    override val isLimitAdTrackingEnabled: Boolean
        get() = false

    override val isWifiConnected: Boolean
        get() = true

    override val appBucket: String
        get() = "active"

    override val appLaunchedFields: JSONObject
        get() {
            val obj = JSONObject()
            obj.put("Build", "1")
            obj.put("Version", "1.0")
            obj.put("OS Version", "11")
            obj.put("SDK Version", 40100)
            obj.put("Make", "Google")
            obj.put("Model", "sdk_gphone_x86")
            obj.put("Carrier", "Android")
            obj.put("useIP", false)
            obj.put("OS", "Android")
            obj.put("wdt", 2.57)
            obj.put("hgt", 4.27)
            obj.put("dpi", 420)
            obj.put("dt", 1)
            obj.put("cc", "us")
            return obj
        }
}
