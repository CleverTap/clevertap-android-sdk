package com.clevertap.android.sdk

import android.content.Context
import org.json.JSONObject

class MockDeviceInfo(
    context: Context?, config: CleverTapInstanceConfig?, cleverTapID: String?,
    coreMetaData: CoreMetaData?
) : DeviceInfo(
    context, config,
    cleverTapID, coreMetaData
) {

    val ctId: String?

    init {
        ctId = cleverTapID
    }

    override fun getDeviceID(): String? {
        return ctId
    }

    override fun onInitDeviceInfo(cleverTapID: String?) {
    }

    override fun getAttributionID(): String {
        return "some-att-id"
    }

    override fun getBluetoothVersion(): String {
        return "2.0"
    }

    override fun getBuild(): Int {
        return 1
    }

    override fun getCarrier(): String {
        return "Android"
    }

    override fun getContext(): Context {
        return super.getContext()
    }

    override fun getCountryCode(): String {
        return "us"
    }

    override fun getDPI(): Int {
        return 420
    }

    override fun getGoogleAdID(): String {
        return "__48703e3cc2ff468ab3c641edf2770d74"
    }

    override fun getHeight(): Double {
        return 4.27
    }

    override fun getHeightPixels(): Int {
        return 1920
    }

    override fun getLibrary(): String {
        return "Android"
    }

    override fun getManufacturer(): String {
        return super.getManufacturer()
    }

    override fun getModel(): String {
        return "sdk_gphone_x86"
    }

    override fun getNetworkType(): String {
        return "4G"
    }

    override fun getNotificationsEnabledForUser(): Boolean {
        return true
    }

    override fun getOsName(): String {
        return "Android"
    }

    override fun getOsVersion(): String {
        return "11"
    }

    override fun getSdkVersion(): Int {
        return 40100
    }

    override fun getVersionName(): String {
        return "1.0"
    }

    override fun getWidth(): Double {
        return 2.57
    }

    override fun getWidthPixels(): Int {
        return 1024
    }

    override fun isBluetoothEnabled(): Boolean {
        return true
    }

    override fun isLimitAdTrackingEnabled(): Boolean {
        return false
    }

    override fun isWifiConnected(): Boolean {
        return true
    }

    override fun getAppBucket(): String {
        return "active"
    }

    override fun getAppLaunchedFields(): JSONObject {
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

    override fun optOutKey(): String {
        return "false"
    }
}