package com.clevertap.android.hms

class TestHmsSdkHandler : IHmsSdkHandler {

    private var isAvailable = false
    private var isSupported = false
    fun setAvailable(available: Boolean) {
        isAvailable = available
    }

    override fun onNewToken(): String {
        return HmsTestConstants.HMS_TOKEN
    }

    override fun appId(): String {
        return if (isAvailable) HmsTestConstants.HMS_APP_ID else HmsTestConstants.EMPTY_STRING
    }

    override fun isSupported(): Boolean {
        return isSupported
    }

    fun setSupported(supported: Boolean) {
        isSupported = supported
    }
}