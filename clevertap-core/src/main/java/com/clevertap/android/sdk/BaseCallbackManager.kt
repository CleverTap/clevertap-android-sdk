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

// Public (restricted) because already-Kotlin public types (e.g. network.FetchInAppListener)
// take it in their signatures.
@RestrictTo(Scope.LIBRARY_GROUP)
public abstract class BaseCallbackManager {

    public abstract fun _notifyInboxInitialized()

    public abstract fun _notifyInboxMessagesDidUpdate()

    internal abstract var failureFlushListener: FailureFlushListener?

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public abstract var featureFlagListener: CTFeatureFlagsListener?

    public abstract var geofenceCallback: GeofenceCallback?

    internal abstract var scDomainListener: SCDomainListener?

    public abstract var inAppNotificationButtonListener: InAppNotificationButtonListener?

    public abstract var inAppNotificationListener: InAppNotificationListener?

    public abstract val pushPermissionResponseListenerList: MutableList<PushPermissionResponseListener?>

    public abstract var inboxListener: CTInboxListener?

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    public abstract var productConfigListener: CTProductConfigListener?

    public abstract var pushAmpListener: CTPushAmpListener?

    public abstract var pushNotificationListener: CTPushNotificationListener?

    public abstract var syncListener: SyncListener?

    public abstract fun notifyDisplayUnitsLoaded(displayUnits: ArrayList<CleverTapDisplayUnit>?)

    //Profile
    public abstract fun notifyUserProfileInitialized(deviceID: String?)

    public abstract fun notifyUserProfileInitialized()

    public abstract fun setDisplayUnitListener(listener: DisplayUnitListener?)

    public abstract fun unregisterPushPermissionResponseListener(pushPermissionResponseListener: PushPermissionResponseListener?)

    public abstract fun registerPushPermissionResponseListener(pushPermissionResponseListener: PushPermissionResponseListener?)

    public abstract fun addOnInitCleverTapIDListener(listener: OnInitCleverTapIDListener?)

    public abstract fun removeOnInitCleverTapIDListener(listener: OnInitCleverTapIDListener?)

    public abstract fun notifyCleverTapIDChanged(id: String?)

    public abstract var fetchVariablesCallback: FetchVariablesCallback?

    public abstract var fetchInAppsCallback: FetchInAppsCallback?

    internal abstract var batchListener: BatchListener?

    internal abstract val changeUserCallbackList: MutableList<ChangeUserCallback?>

    internal abstract fun addChangeUserCallback(callback: ChangeUserCallback?)

    internal abstract fun removeChangeUserCallback(callback: ChangeUserCallback?)
}
