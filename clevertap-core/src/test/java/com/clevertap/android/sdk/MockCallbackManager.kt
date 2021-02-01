package com.clevertap.android.sdk

import com.clevertap.android.sdk.InAppNotificationActivity.InAppActivityListener
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.product_config.CTProductConfigListener
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener
import java.lang.ref.WeakReference

class MockCallbackManager :
    BaseCallbackManager() {

    override fun _notifyInboxMessagesDidUpdate() {
        TODO("Not yet implemented")
    }

    override fun getFailureFlushListener(): FailureFlushListener {
        TODO("Not yet implemented")
    }

    override fun getFeatureFlagListener(): CTFeatureFlagsListener {
        TODO("Not yet implemented")
    }

    override fun getGeofenceCallback(): GeofenceCallback {
        TODO("Not yet implemented")
    }

    override fun getInAppActivityListener(): InAppActivityListener {
        TODO("Not yet implemented")
    }

    override fun getInAppNotificationButtonListener(): InAppNotificationButtonListener {
        TODO("Not yet implemented")
    }

    override fun getInAppNotificationListener(): InAppNotificationListener {
        TODO("Not yet implemented")
    }

    override fun getInboxListener(): CTInboxListener {
        TODO("Not yet implemented")
    }

    override fun getProductConfigListener(): WeakReference<CTProductConfigListener> {
        TODO("Not yet implemented")
    }

    override fun getPushAmpListener(): CTPushAmpListener {
        TODO("Not yet implemented")
    }

    override fun getPushNotificationListener(): CTPushNotificationListener {
        TODO("Not yet implemented")
    }

    override fun getSyncListener(): SyncListener {
        TODO("Not yet implemented")
    }

    override fun notifyUserProfileInitialized(deviceID: String?) {
        TODO("Not yet implemented")
    }

    override fun setDisplayUnitListener(listener: DisplayUnitListener?) {
        TODO("Not yet implemented")
    }

    override fun setFailureFlushListener(failureFlushListener: FailureFlushListener?) {
        TODO("Not yet implemented")
    }

    override fun setFeatureFlagListener(listener: CTFeatureFlagsListener?) {
        TODO("Not yet implemented")
    }

    override fun setGeofenceCallback(geofenceCallback: GeofenceCallback?) {
        TODO("Not yet implemented")
    }

    override fun setInAppActivityListener(inAppActivityListener: InAppActivityListener?) {
        TODO("Not yet implemented")
    }

    override fun setInAppNotificationButtonListener(inAppNotificationButtonListener: InAppNotificationButtonListener?) {
        TODO("Not yet implemented")
    }

    override fun setInAppNotificationListener(inAppNotificationListener: InAppNotificationListener?) {
        TODO("Not yet implemented")
    }

    override fun setInboxListener(inboxListener: CTInboxListener?) {
        TODO("Not yet implemented")
    }

    override fun setProductConfigListener(productConfigListener: CTProductConfigListener?) {
        TODO("Not yet implemented")
    }

    override fun setPushAmpListener(pushAmpListener: CTPushAmpListener?) {
        TODO("Not yet implemented")
    }

    override fun setPushNotificationListener(pushNotificationListener: CTPushNotificationListener?) {
        TODO("Not yet implemented")
    }

    override fun setSyncListener(syncListener: SyncListener?) {
        TODO("Not yet implemented")
    }
}