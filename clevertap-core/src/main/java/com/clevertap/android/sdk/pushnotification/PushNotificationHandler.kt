package com.clevertap.android.sdk.pushnotification

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.interfaces.ActionButtonClickHandler
import com.clevertap.android.sdk.interfaces.NotificationHandler
import com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAccountIdFromNotificationBundle

open class PushNotificationHandler private constructor() : ActionButtonClickHandler {

    private object SingletonNotificationHandler {

        val INSTANCE = PushNotificationHandler()
    }

    private fun isForSignedCall(extras: Bundle?): Boolean {
        if (extras == null) {
            return false
        }
        val source = extras.getString("source")
        return "signedcall" == source
    }

    override fun onActionButtonClick(context: Context?, extras: Bundle?, notificationId: Int): Boolean {
        return false
    }

    @Synchronized
    override fun onMessageReceived(
        applicationContext: Context?,
        message: Bundle?,
        pushType: String?
    ): Boolean {
        message!!.putLong(Constants.OMR_INVOKE_TIME_IN_MILLIS, System.currentTimeMillis())
        val cleverTapAPI = CleverTapAPI
            .getGlobalInstance(applicationContext, getAccountIdFromNotificationBundle(message))
        val info = CleverTapAPI.getNotificationInfo(message)

        if (info.fromCleverTap) {
            if (cleverTapAPI != null) {
                cleverTapAPI.coreState.config.log(
                    LOG_TAG,
                    pushType + "received notification from CleverTap: " + message.toString()
                )
                // Live Update: surface the single nested `data` object (incl. pt_id, if any) to the
                // top level so the normal PT/core routing below can read it. A `pt_id` inside `data`
                // selects Mode B (SDK/PT render); its absence means Mode A (client factory). The
                // Mode A vs Mode B decision is finalised in _createNotification.
                if (LiveUpdateRouter.isLiveUpdate(message)) {
                    surfaceLiveUpdatePayload(message)
                }
                if (isForPushTemplates(message) && CleverTapAPI.getNotificationHandler() != null) {
                    // render push template
                    CleverTapAPI.getNotificationHandler().onMessageReceived(applicationContext, message, pushType)
                } else if (isForSignedCall(message) && CleverTapAPI.getSignedCallNotificationHandler() != null) {
                    // handle voip push payload
                    CleverTapAPI.getSignedCallNotificationHandler()
                        .onMessageReceived(applicationContext, message, pushType)
                } else {
                    // render core push
                    cleverTapAPI.renderPushNotificationOnCallerThread(
                        CoreNotificationRenderer(),
                        applicationContext,
                        message
                    )
                    //CleverTapAPI.createNotification(applicationContext, message);
                }
            } else {
                Logger.d(LOG_TAG, pushType + "received notification from CleverTap: " + message.toString())
                Logger.d(LOG_TAG, "$pushType not renderning since cleverTapAPI is null")
            }
            return true
        }

        return false
    }

    override fun onNewToken(applicationContext: Context?, token: String?, pushType: PushType?): Boolean {
        CleverTapAPI.tokenRefresh(applicationContext, token, pushType)
        return true
    }

    companion object {

        @JvmStatic
        fun getPushNotificationHandler(): NotificationHandler {
            return SingletonNotificationHandler.INSTANCE
        }

        /**
         * Merges the single nested `data` JSON object of a Live Update push into the top-level
         * bundle so downstream routing (Mode B / Push Template selection via `pt_id`) and rendering
         * read it like a normal push, and so a Mode A factory can read the fields as flat extras. The
         * presence of `pt_id` inside `data` distinguishes Mode B (SDK/PT render) from Mode A
         * (client factory).
         *
         * The flatten semantics (string coercion, compact-JSON for nested arrays/objects, JSON-null
         * skip, and root-wins so wrapper/identity/analytics keys are never overwritten) live in
         * [LiveUpdatePayloadSurfacer.flatten] — a pure, unit-tested seam. This method just applies
         * the result to the bundle.
         */
        internal fun surfaceLiveUpdatePayload(message: Bundle) {
            val data = message.getString(Constants.WZRK_LIVE_UPDATE_DATA)
            val toSurface = LiveUpdatePayloadSurfacer.flatten(data, message.keySet())
            for ((key, value) in toSurface) {
                message.putString(key, value)
            }
        }

        @JvmStatic
        fun isForPushTemplates(extras: Bundle?): Boolean {
            if (extras == null) {
                return false
            }
            val ptId = extras.getString("pt_id")
            return !("0" == ptId || ptId == null || ptId.isEmpty())
        }
    }
}
