package com.clevertap.android.sdk

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.clevertap.android.sdk.Utils.runOnUiThread
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
import java.lang.ref.WeakReference
import java.util.Collections

@RestrictTo(Scope.LIBRARY)
internal open class CallbackManager(
    private val config: CleverTapInstanceConfig,
    private val deviceInfo: DeviceInfo
) : BaseCallbackManager() {

    private var displayUnitListenerWeakReference: WeakReference<DisplayUnitListener?>? = null

    override var geofenceCallback: GeofenceCallback? = null

    override var scDomainListener: SCDomainListener? = null

    private var inAppNotificationButtonListenerWeakReference: WeakReference<InAppNotificationButtonListener?>? = null

    override var inAppNotificationButtonListener: InAppNotificationButtonListener?
        get() = inAppNotificationButtonListenerWeakReference?.get()
        set(listener) {
            inAppNotificationButtonListenerWeakReference = WeakReference(listener)
        }

    override var inAppNotificationListener: InAppNotificationListener? = null

    override val pushPermissionResponseListenerList: MutableList<PushPermissionResponseListener?> = ArrayList()

    override var inboxListener: CTInboxListener? = null

    override var failureFlushListener: FailureFlushListener? = null

    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    private var featureFlagListenerWeakReference: WeakReference<CTFeatureFlagsListener?>? = null

    private val onInitCleverTapIDListeners: MutableList<OnInitCleverTapIDListener?> =
        Collections.synchronizedList(ArrayList())

    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    private var productConfigListenerWeakReference: WeakReference<CTProductConfigListener?>? = null

    override var pushAmpListener: CTPushAmpListener? = null

    override var pushNotificationListener: CTPushNotificationListener? = null

    override var syncListener: SyncListener? = null

    override var fetchInAppsCallback: FetchInAppsCallback? = null

    override val changeUserCallbackList: MutableList<ChangeUserCallback?> =
        Collections.synchronizedList(ArrayList())

    override var fetchVariablesCallback: FetchVariablesCallback? = null

    override var batchListener: BatchListener? = null

    override fun _notifyInboxMessagesDidUpdate() {
        if (inboxListener != null) {
            runOnUiThread {
                inboxListener?.inboxMessagesDidUpdate()
            }
        }
    }

    override fun addChangeUserCallback(callback: ChangeUserCallback?) {
        changeUserCallbackList.add(callback)
    }

    override fun removeChangeUserCallback(callback: ChangeUserCallback?) {
        changeUserCallbackList.remove(callback)
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    @Suppress("DEPRECATION")
    override var featureFlagListener: CTFeatureFlagsListener?
        get() = featureFlagListenerWeakReference?.get()
        set(listener) {
            featureFlagListenerWeakReference = WeakReference(listener)
        }

    override fun registerPushPermissionResponseListener(
        pushPermissionResponseListener: PushPermissionResponseListener?
    ) {
        pushPermissionResponseListenerList.add(pushPermissionResponseListener)
    }

    override fun unregisterPushPermissionResponseListener(
        pushPermissionResponseListener: PushPermissionResponseListener?
    ) {
        pushPermissionResponseListenerList.remove(pushPermissionResponseListener)
    }

    /**
     * Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     */
    @Deprecated("This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.")
    @Suppress("DEPRECATION")
    override var productConfigListener: CTProductConfigListener?
        get() = productConfigListenerWeakReference?.get()
        set(productConfigListener) {
            if (productConfigListener != null) {
                productConfigListenerWeakReference = WeakReference(productConfigListener)
            }
        }

    override fun addOnInitCleverTapIDListener(listener: OnInitCleverTapIDListener?) {
        onInitCleverTapIDListeners.add(listener)
    }

    override fun removeOnInitCleverTapIDListener(listener: OnInitCleverTapIDListener?) {
        onInitCleverTapIDListeners.remove(listener)
    }

    override fun notifyCleverTapIDChanged(id: String?) {
        synchronized(onInitCleverTapIDListeners) {
            onInitCleverTapIDListeners.forEach { it?.onInitCleverTapID(id) }
        }
    }

    //Profile
    override fun notifyUserProfileInitialized(deviceID: String?) {
        val resolvedDeviceID = deviceID ?: deviceInfo.deviceID ?: return
        try {
            syncListener?.profileDidInitialize(resolvedDeviceID)
        } catch (t: Throwable) {
            // Ignore
        }
    }

    override fun setDisplayUnitListener(listener: DisplayUnitListener?) {
        if (listener != null) {
            displayUnitListenerWeakReference = WeakReference(listener)
        } else {
            config.logger.verbose(
                config.accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}Failed to set - DisplayUnitListener can't be null"
            )
        }
    }

    override fun _notifyInboxInitialized() {
        inboxListener?.inboxDidInitialize()
    }

    /**
     * Notify the registered Display Unit listener about the running Display Unit campaigns
     *
     * @param displayUnits - Array of Display Units [CleverTapDisplayUnit]
     */
    override fun notifyDisplayUnitsLoaded(displayUnits: ArrayList<CleverTapDisplayUnit>?) {
        when {
            displayUnits.isNullOrEmpty() -> config.logger.verbose(
                config.accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}No Display Units found"
            )

            displayUnitListenerWeakReference?.get() != null -> runOnUiThread {
                //double check to ensure null safety
                displayUnitListenerWeakReference?.get()?.onDisplayUnitsLoaded(displayUnits)
            }

            else -> config.logger.verbose(
                config.accountId,
                "${Constants.FEATURE_DISPLAY_UNIT}No registered listener, failed to notify"
            )
        }
    }

    override fun notifyUserProfileInitialized() {
        notifyUserProfileInitialized(deviceInfo.deviceID)
    }
}
