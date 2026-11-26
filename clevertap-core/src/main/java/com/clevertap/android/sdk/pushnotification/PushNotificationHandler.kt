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

public open class PushNotificationHandler private constructor() : ActionButtonClickHandler {

    private object SingletonNotificationHandler {

        val INSTANCE = PushNotificationHandler()
    }

    private fun isForSignedCall(extras: Bundle?): Boolean = extras?.getString("source") == "signedcall"

    override fun onActionButtonClick(context: Context?, extras: Bundle?, notificationId: Int): Boolean = false

    @Synchronized
    override fun onMessageReceived(
        applicationContext: Context?,
        message: Bundle?,
        pushType: String?
    ): Boolean {
        // A null message threw a NullPointerException in Java; `!!` keeps that.
        message!!.putLong(Constants.OMR_INVOKE_TIME_IN_MILLIS, System.currentTimeMillis())
        val cleverTapAPI = CleverTapAPI
            .getGlobalInstance(applicationContext, getAccountIdFromNotificationBundle(message))
        val info = CleverTapAPI.getNotificationInfo(message)

        if (!info.fromCleverTap) {
            return false
        }
        if (cleverTapAPI == null) {
            Logger.d(LOG_TAG, "${pushType}received notification from CleverTap: $message")
            Logger.d(LOG_TAG, "$pushType not renderning since cleverTapAPI is null")
            return true
        }
        cleverTapAPI.coreState.config.log(LOG_TAG, "${pushType}received notification from CleverTap: $message")
        // Live Update: surface the single nested `data` object (incl. pt_id, if any) to the
        // top level so the normal PT/core routing below can read it. A `pt_id` inside `data`
        // selects Mode B (SDK/PT render); its absence means Mode A (client factory). The
        // Mode A vs Mode B decision is finalised in _createNotification.
        if (LiveUpdateRouter.isLiveUpdate(message)) {
            surfaceLiveUpdatePayload(message)
        }
        // A push template handler renders push templates, a signed call handler handles voip push
        // payloads; everything else is rendered by core.
        val delegate = (if (isForPushTemplates(message)) { CleverTapAPI.getNotificationHandler() } else { null })
            ?: (if (isForSignedCall(message)) { CleverTapAPI.getSignedCallNotificationHandler() } else { null })
        if (delegate != null) {
            delegate.onMessageReceived(applicationContext, message, pushType)
        } else {
            cleverTapAPI.renderPushNotificationOnCallerThread(
                CoreNotificationRenderer(),
                applicationContext,
                message
            )
        }
        return true
    }

    override fun onNewToken(applicationContext: Context?, token: String?, pushType: PushType?): Boolean {
        CleverTapAPI.tokenRefresh(applicationContext, token, pushType)
        return true
    }

    public companion object {

        @JvmStatic
        public fun getPushNotificationHandler(): NotificationHandler = SingletonNotificationHandler.INSTANCE

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
            toSurface.forEach { (key, value) -> message.putString(key, value) }
        }

        @JvmStatic
        public fun isForPushTemplates(extras: Bundle?): Boolean {
            val ptId = extras?.getString("pt_id")
            return !ptId.isNullOrEmpty() && ptId != "0"
        }
    }
}
