package com.clevertap.android.sdk.features

import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.android.sdk.pushnotification.INotificationRenderer
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.pushnotification.PushType
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.response.PushAmpResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Push notification feature
 * Manages push notification providers (FCM, HMS, etc.)
 */
internal class PushFeature(
    val notifyNotificationRendered: (String) -> Unit = { listenerKey ->
        val nrl = CleverTapAPI.getNotificationRenderedListener(listenerKey)
        nrl?.onNotificationRendered(true)
    }
) : CleverTapFeature {

    lateinit var coreContract: CoreContract
    var pushAmpListener: CTPushAmpListener? = null
    var pushNotificationListener: CTPushNotificationListener? = null

    // Lazy-initialized Push dependencies (initialized after coreContract is set)
    private val ctWorkManager: CTWorkManager by lazy {
        CTWorkManager(coreContract.context(), coreContract.config())
    }

    val pushProviders: PushProviders by lazy {
        PushProviders(
            coreContract.context(),
            coreContract.config(),
            coreContract.database(),
            coreContract.validationResultStack(),
            coreContract.analytics(),
            ctWorkManager,
            coreContract.executors(),
            coreContract.clock()
        )
    }

    val pushAmpResponse: PushAmpResponse by lazy {
        PushAmpResponse(
            coreContract.config().accountId,
            coreContract.logger()
        )
    }

    override fun handleApiData(
        response: JSONObject,
        isFullResponse: Boolean,
        isUserSwitching: Boolean
    ) {
        // Handle Pull Notifications response
        if (coreContract.config().isAnalyticsOnly) {
            coreContract.logger().verbose(
                coreContract.config().accountId,
                "CleverTap instance is configured to analytics only, not processing push amp response"
            )
            return
        }
        pushAmpResponse.processResponse(response, coreContract.context(), coreContract.database(), pushProviders, pushAmpListener)
    }

    /**
     * Handles the click event of a push notification.
     *
     * Raises a "Notification Clicked" event to be sent
     * to the CleverTap backend and notifies the [CTPushNotificationListener] if one is registered.
     *
     * @param extras The bundle data received from the push notification payload.
     */
    fun handlePushNotificationClicked(extras: Bundle) {
        // Regular push notification click - track via analytics
        val sent: Boolean = coreContract.analytics().pushNotificationClickedEvent(extras)
        if (sent) {
            pushNotificationListener?.onNotificationClickedPayloadReceived(
                Utils.convertBundleObjectToHashMap(extras)
            )
        }
    }

    /**
     * Phase 3: Push impression tracking moved from CoreState
     * Notifies listeners when push impressions are sent to server
     */
    fun notifyPushImpressionsSentToServer(queue: JSONArray) {
        for (i in 0..<queue.length()) {
            try {
                val notif = queue.getJSONObject(i).optJSONObject("evtData")
                if (notif != null) {
                    val pushId = notif.optString(Constants.WZRK_PUSH_ID)
                    val pushAccountId = notif.optString(Constants.WZRK_ACCT_ID_KEY)

                    val key = PushNotificationUtil
                        .buildPushNotificationRenderedListenerKey(pushAccountId, pushId)
                    notifyNotificationRendered(key)
                }
            } catch (_: JSONException) {
                coreContract.logger().verbose(
                    coreContract.config().accountId,
                    "Encountered an exception while parsing the push notification viewed event queue"
                )
            }
        }

        coreContract.logger().verbose(
            coreContract.config().accountId,
            "push notification viewed event sent successfully"
        )
    }

    // ========== PUBLIC API FACADES ==========
    // These methods provide direct delegation from CleverTapAPI to Push functionality
    // Signature matches CleverTapAPI public methods for 1:1 mapping

    /**
     * Sends the FCM registration ID to CleverTap
     */
    fun pushFcmRegistrationId(fcmId: String?, register: Boolean) {
        pushProviders.handleToken(fcmId, PushConstants.FCM, register)
    }

    /**
     * Sends push registration token for the given push type
     */
    fun pushRegistrationToken(
        token: String?,
        pushType: PushType?,
        register: Boolean
    ) {
        pushProviders.handleToken(token, pushType, register)
    }

    /**
     * Returns the device push token or null
     */
    fun getPushToken(pushType: PushType): String? {
        return pushProviders.getCachedToken(pushType)
    }

    /**
     * Returns the device push token or null (for backward compatibility)
     */
    fun getDevicePushToken(type: PushType): String? {
        return pushProviders.getCachedToken(type)
    }

    /**
     * Returns the DevicePushTokenRefreshListener
     */
    fun getDevicePushTokenRefreshListener(): CleverTapAPI.DevicePushTokenRefreshListener? {
        return pushProviders.devicePushTokenRefreshListener
    }

    /**
     * Sets the DevicePushTokenRefreshListener object
     */
    fun setDevicePushTokenRefreshListener(tokenRefreshListener: CleverTapAPI.DevicePushTokenRefreshListener?) {
        pushProviders.devicePushTokenRefreshListener = tokenRefreshListener
    }

    /**
     * Sets the RequestDevicePushTokenListener object
     */
    fun setRequestDevicePushTokenListener(requestTokenListener: CleverTapAPI.RequestDevicePushTokenListener?) {
        try {
            coreContract.logger().verbose(
                PushConstants.LOG_TAG,
                PushConstants.FCM_LOG_TAG + "Requesting FCM token using googleservices.json"
            )
            com.google.firebase.messaging.FirebaseMessaging
                .getInstance()
                .token
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        coreContract.logger().verbose(
                            PushConstants.LOG_TAG,
                            PushConstants.FCM_LOG_TAG + "FCM token using googleservices.json failed",
                            task.exception
                        )
                        requestTokenListener?.onDevicePushToken(null, PushConstants.FCM)
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    coreContract.logger().verbose(
                        PushConstants.LOG_TAG,
                        PushConstants.FCM_LOG_TAG + "FCM token using googleservices.json - $token"
                    )
                    requestTokenListener?.onDevicePushToken(token, PushConstants.FCM)
                }

        } catch (t: Throwable) {
            coreContract.logger().verbose(
                PushConstants.LOG_TAG,
                PushConstants.FCM_LOG_TAG + "Error requesting FCM token", t
            )
            requestTokenListener?.onDevicePushToken(null, PushConstants.FCM)
        }
    }

    /**
     * Renders push notification with custom renderer
     */
    fun renderPushNotification(
        iNotificationRenderer: INotificationRenderer,
        context: android.content.Context,
        extras: Bundle?
    ): java.util.concurrent.Future<*>? {
        val config = coreContract.config()
        var future: java.util.concurrent.Future<*>? = null

        try {
            val task = coreContract.executors().postAsyncSafelyTask<Void>()
            future = task.submit("CleverTapAPI#renderPushNotification") {
                synchronized(pushProviders.pushRenderingLock) {
                    pushProviders.pushNotificationRenderer = iNotificationRenderer

                    if (extras != null && extras.containsKey(Constants.PT_NOTIF_ID)) {
                        pushProviders._createNotification(
                            context, extras,
                            extras.getInt(Constants.PT_NOTIF_ID)
                        )
                    } else {
                        pushProviders._createNotification(context, extras, Constants.EMPTY_NOTIFICATION_ID)
                    }
                }
                null
            }
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "Failed to process renderPushNotification()", t)
        }

        return future
    }

    /**
     * Renders push notification on caller thread with custom renderer
     */
    fun renderPushNotificationOnCallerThread(
        iNotificationRenderer: INotificationRenderer,
        context: android.content.Context,
        extras: Bundle?
    ) {
        val config = coreContract.config()

        try {
            synchronized(pushProviders.pushRenderingLock) {
                config.logger.verbose(
                    config.accountId,
                    "rendering push on caller thread with id = ${Thread.currentThread().id}"
                )
                pushProviders.pushNotificationRenderer = iNotificationRenderer

                if (extras != null && extras.containsKey(Constants.PT_NOTIF_ID)) {
                    pushProviders._createNotification(
                        context, extras,
                        extras.getInt(Constants.PT_NOTIF_ID)
                    )
                } else {
                    pushProviders._createNotification(context, extras, Constants.EMPTY_NOTIFICATION_ID)
                }
            }
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "Failed to process renderPushNotification()", t)
        }
    }

    /**
     * Process custom push notification
     */
    fun processPushNotification(extras: Bundle?) {
        pushProviders.processCustomPushNotification(extras)
    }
}
