package com.clevertap.android.sdk

import android.os.Bundle
import com.clevertap.android.sdk.inapp.CTInAppNotification
import org.json.JSONObject
import java.util.ArrayList

class MockAnalyticsManager : BaseAnalyticsManager() {

    override fun addMultiValuesForKey(key: String, values: ArrayList<String>) {}
    override fun incrementValue(key: String, value: Number) {

    }
    override fun decrementValue(key: String, value: Number) {

    }

    override fun fetchFeatureFlags() {}
    override fun forcePushAppLaunchedEvent() {}
    override fun pushAppLaunchedEvent() {}
    override fun pushDisplayUnitClickedEventForID(unitID: String) {}
    override fun pushDisplayUnitViewedEventForID(unitID: String) {}
    override fun pushError(errorMessage: String, errorCode: Int) {}
    override fun pushEvent(eventName: String, eventActions: Map<String, Any>) {}
    override fun pushInAppNotificationStateEvent(
        clicked: Boolean, data: CTInAppNotification,
        customData: Bundle
    ) {
    }

    override fun pushInstallReferrer(url: String) {}
    override fun pushInstallReferrer(source: String, medium: String, campaign: String) {}
    override fun pushNotificationClickedEvent(extras: Bundle) {}
    override fun pushNotificationViewedEvent(extras: Bundle) {}
    override fun pushProfile(profile: Map<String, Any>) {}
    override fun removeMultiValuesForKey(key: String, values: ArrayList<String>) {}
    override fun removeValueForKey(key: String) {}
    override fun sendDataEvent(event: JSONObject) {}
    override fun sendFetchEvent(eventObject: JSONObject?) {
        TODO("Not yet implemented")
    }
}