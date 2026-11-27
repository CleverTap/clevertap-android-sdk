package com.clevertap.android.sdk

import android.os.Bundle
import com.clevertap.android.sdk.inapp.CTInAppNotification
import org.json.JSONObject
import androidx.annotation.RestrictTo

@Suppress("UnnecessaryAbstractClass") // intentional base of AnalyticsManager, config/flag controllers, fakes
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class BaseAnalyticsManager {

    abstract fun addMultiValuesForKey(key: String?, values: List<String?>?)

    abstract fun incrementValue(key: String?, value: Number?)

    abstract fun decrementValue(key: String?, value: Number?)

    abstract fun fetchFeatureFlags()

    //Event
    abstract fun forcePushAppLaunchedEvent()

    abstract fun pushAppLaunchedEvent()

    abstract fun pushDefineVarsEvent(data: JSONObject)

    abstract fun pushDisplayUnitClickedEventForID(unitID: String?)

    abstract fun pushDisplayUnitElementClickedEventForID(
        unitID: String?, additionalProperties: Map<String, Any>?
    )

    abstract fun pushDisplayUnitElementViewedEventForID(
        unitID: String?, additionalProperties: Map<String, Any>?
    )

    abstract fun pushDisplayUnitViewedEventForID(unitID: String?)

    abstract fun pushError(errorMessage: String?, errorCode: Int)

    abstract fun pushEvent(eventName: String?, eventActions: Map<String, Any?>?)

    abstract fun pushInAppNotificationStateEvent(
        clicked: Boolean, data: CTInAppNotification?,
        customData: Bundle?
    )

    abstract fun pushInstallReferrer(url: String?)

    abstract fun pushInstallReferrer(source: String?, medium: String?, campaign: String?)

    abstract fun pushNotificationClickedEvent(extras: Bundle?)

    abstract fun pushNotificationViewedEvent(extras: Bundle?)

    abstract fun pushProfile(profile: Map<String, Any?>?)

    abstract fun removeMultiValuesForKey(key: String?, values: List<String?>?)

    abstract fun removeValueForKey(key: String?)

    abstract fun sendDataEvent(event: JSONObject)

    abstract fun sendFetchEvent(eventObject: JSONObject)
}
