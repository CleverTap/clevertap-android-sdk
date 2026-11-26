package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.inapp.callbacks.FetchInAppsCallback
import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener
import com.clevertap.android.sdk.interfaces.SCDomainListener
import com.clevertap.android.sdk.login.ChangeUserCallback
import com.clevertap.android.sdk.network.BatchListener
import com.clevertap.android.sdk.product_config.CTProductConfigListener
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class BaseCallbackManager {

    abstract fun notifyInboxInitialized()

    abstract fun notifyInboxMessagesDidUpdate()

    internal abstract var failureFlushListener: FailureFlushListener?

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    abstract var featureFlagListener: CTFeatureFlagsListener?

    abstract var geofenceCallback: GeofenceCallback?

    internal abstract var scDomainListener: SCDomainListener?

    abstract var inAppNotificationButtonListener: InAppNotificationButtonListener?

    abstract var inAppNotificationListener: InAppNotificationListener?

    abstract val pushPermissionResponseListenerList: MutableList<PushPermissionResponseListener?>

    abstract var inboxListener: CTInboxListener?

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    abstract var productConfigListener: CTProductConfigListener?

    abstract var pushAmpListener: CTPushAmpListener?

    abstract var pushNotificationListener: CTPushNotificationListener?

    abstract var syncListener: SyncListener?

    abstract fun notifyDisplayUnitsLoaded(displayUnits: ArrayList<CleverTapDisplayUnit>?)

    //Profile
    abstract fun notifyUserProfileInitialized(deviceID: String?)

    abstract fun notifyUserProfileInitialized()

    abstract fun setDisplayUnitListener(listener: DisplayUnitListener?)

    abstract fun unregisterPushPermissionResponseListener(
        pushPermissionResponseListener: PushPermissionResponseListener?
    )

    abstract fun registerPushPermissionResponseListener(pushPermissionResponseListener: PushPermissionResponseListener?)

    abstract fun addOnInitCleverTapIDListener(listener: OnInitCleverTapIDListener?)

    abstract fun removeOnInitCleverTapIDListener(listener: OnInitCleverTapIDListener?)

    abstract fun notifyCleverTapIDChanged(id: String?)

    abstract var fetchVariablesCallback: FetchVariablesCallback?

    abstract var fetchInAppsCallback: FetchInAppsCallback?

    internal abstract var batchListener: BatchListener?

    internal abstract val changeUserCallbackList: MutableList<ChangeUserCallback?>

    internal abstract fun addChangeUserCallback(callback: ChangeUserCallback?)

    internal abstract fun removeChangeUserCallback(callback: ChangeUserCallback?)
}
