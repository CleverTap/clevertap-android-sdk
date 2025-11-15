package com.clevertap.android.sdk.features.callbacks

import android.os.Bundle
import com.clevertap.android.sdk.InAppNotificationButtonListener
import com.clevertap.android.sdk.InAppNotificationListener
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.PushPermissionResponseListener
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.callbacks.FetchInAppsCallback
import org.json.JSONObject

/**
 * Manages all InApp-related callbacks
 * Part of the InApp feature layer
 */
internal class InAppCallbackManager {

    // InApp notification lifecycle listener
    @Volatile
    private var inAppNotificationListener: InAppNotificationListener? = null

    // InApp button interaction listener
    @Volatile
    private var inAppNotificationButtonListener: InAppNotificationButtonListener? = null

    // Fetch InApps callback
    @Volatile
    private var fetchInAppsCallback: FetchInAppsCallback? = null

    private val pushPermRespListeners: MutableList<PushPermissionResponseListener> = mutableListOf()

    // Setters
    fun setInAppNotificationListener(listener: InAppNotificationListener?) {
        this.inAppNotificationListener = listener
    }

    fun setInAppNotificationButtonListener(listener: InAppNotificationButtonListener?) {
        this.inAppNotificationButtonListener = listener
    }

    fun setFetchInAppsCallback(callback: FetchInAppsCallback?) {
        this.fetchInAppsCallback = callback
    }

    // Getters
    fun getInAppNotificationListener(): InAppNotificationListener? = inAppNotificationListener

    fun getFetchInAppsCallback(): FetchInAppsCallback? = fetchInAppsCallback

    // Helper methods for invoking callbacks with proper error handling
    fun notifyInAppShown(inAppNotification: CTInAppNotification) {
        try {
            inAppNotificationListener?.onShow(inAppNotification)
        } catch (t: Throwable) {
            // Log error but don't crash
            Logger.v("InAppCallbackManager", "Error in onShow callback", t)
        }
    }

    fun clientBeforeShow(extras: JSONObject?): Boolean {
        val il = inAppNotificationListener
        if (il == null) {
            return true
        }
        return try {
            val kvs = if (extras != null) {
                Utils.convertJSONObjectToHashMap(extras)
            } else {
                HashMap<String, Any>()
            }
            il.beforeShow(kvs)
        } catch (t: Throwable) {
            Logger.v("InAppCallbackManager", "Error in beforeShow callback", t)
            true // Default to showing if callback fails
        }
    }

    fun notifyButtonClick(keyValues: HashMap<String, String>?) {
        try {
            inAppNotificationButtonListener?.onInAppButtonClick(keyValues)
        } catch (t: Throwable) {
            Logger.v("InAppCallbackManager", "Error in button click callback", t)
        }
    }

    fun notifyInAppDismissed(extras: JSONObject?, formData: Bundle?) {
        val il = inAppNotificationListener
        if (il == null) {
            return
        }
        try {
            val jsonToMap: HashMap<String, Any> = if (extras != null) {
                Utils.convertJSONObjectToHashMap(extras)
            } else {
                HashMap()
            }
            val bundleToMap = if (formData != null) {
                Utils.convertBundleObjectToHashMap(formData)
            } else {
                null
            }
            il.onDismissed(jsonToMap, bundleToMap)
        } catch (t: Throwable) {
            Logger.v("InAppCallbackManager", "Error in onDismissed callback", t)
        }
    }

    fun getPushPermissionResponseListenerList(): List<PushPermissionResponseListener> {
        return pushPermRespListeners
    }

    fun registerPushPermissionResponseListener(pushPermissionResponseListener: PushPermissionResponseListener) {
        this.pushPermRespListeners.add(pushPermissionResponseListener)
    }

    fun unregisterPushPermissionResponseListener(pushPermissionResponseListener: PushPermissionResponseListener) {
        this.pushPermRespListeners.remove(pushPermissionResponseListener)
    }
}