package com.clevertap.android.sdk.inapp

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.utils.UriHelper
import java.lang.ref.WeakReference
import java.net.URLDecoder
import kotlin.text.split

internal class CTInAppHost(
    inAppListener: InAppListener?,
    private val config: CleverTapInstanceConfig,
    private val inAppNotification: CTInAppNotification,
    private var callbacks: Callbacks?,
    context: Context?
) {

    internal interface Callbacks {
        fun onDismissInApp()
    }

    private var inAppListenerWeakReference = WeakReference(inAppListener)
    private val contextWeakReference = WeakReference(context)

    fun getInAppListener(): InAppListener? {
        val listener = inAppListenerWeakReference.get()
        if (listener == null) {
            config.logger.verbose(
                config.accountId,
                "InAppListener is null for notification: ${inAppNotification.jsonDescription}"
            )
        }
        return listener
    }

    fun setInAppListener(listener: InAppListener) {
        inAppListenerWeakReference = WeakReference(listener)
    }

    fun getScaledPixels(raw: Int): Int {
        val context = contextWeakReference.get()
        if (context == null) {
            return raw
        }
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            raw.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    fun triggerAction(
        action: CTInAppAction, callToAction: String?, additionalData: Bundle?
    ) {
        var additionalData = additionalData
        var action = action
        var callToAction = callToAction
        if (action.type == InAppActionType.OPEN_URL) {
            //All URL parameters should be tracked as additional data
            val urlActionData = UriHelper.getAllKeyValuePairs(action.actionUrl, false)

            // callToAction is handled as a parameter
            var callToActionUrlParam = urlActionData.getString(Constants.KEY_C2A)
            // no need to keep it in the data bundle
            urlActionData.remove(Constants.KEY_C2A)

            // add all additional params, overriding the url params if there is a collision
            if (additionalData != null) {
                urlActionData.putAll(additionalData)
            }
            // Use the merged data for the action
            additionalData = urlActionData
            if (callToActionUrlParam != null) {
                // check if there is a deeplink within the callToAction param
                val parts = callToActionUrlParam.split(Constants.URL_PARAM_DL_SEPARATOR)
                if (parts.size == 2) {
                    // Decode it here as it is not decoded by UriHelper
                    try {
                        // Extract the actual callToAction value
                        callToActionUrlParam = URLDecoder.decode(parts[0], "UTF-8")
                    } catch (e: Exception) {
                        config.logger.debug("Error parsing c2a param", e)
                    }
                    // use the url from the callToAction param
                    action = CTInAppAction.CREATOR.createOpenUrlAction(parts[1])
                }
            }
            if (callToAction == null) {
                // Use the url param value only if no other value is passed
                callToAction = callToActionUrlParam
            }
        }
        val actionData = notifyActionTriggered(action, callToAction ?: "", additionalData)
        didDismissInApp(actionData)
    }

    fun openUrl(url: String) {
        triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, null)
    }

    fun didDismissInApp(data: Bundle?) {
        callbacks?.onDismissInApp()
        getInAppListener()?.inAppNotificationDidDismiss(inAppNotification, data)
    }

    fun didShowInApp(data: Bundle?) {
        getInAppListener()?.inAppNotificationDidShow(inAppNotification, data)
    }

    fun notifyActionTriggered(
        action: CTInAppAction, callToAction: String, additionalData: Bundle?
    ): Bundle? {
        return getInAppListener()?.inAppNotificationActionTriggered(
            inAppNotification, action, callToAction, additionalData, contextWeakReference.get()
        )
    }
}
