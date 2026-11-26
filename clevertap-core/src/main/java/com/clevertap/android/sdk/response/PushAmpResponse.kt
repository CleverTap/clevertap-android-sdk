package com.clevertap.android.sdk.response

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler
import com.clevertap.android.sdk.utils.CTJsonConverter.pushIdsToJSONArray
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class PushAmpResponse(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val baseDatabaseManager: BaseDatabaseManager,
    private val callbackManager: BaseCallbackManager,
    private val controllerManager: ControllerManager
) : CleverTapResponseDecorator() {

    private val logger: Logger = config.logger

    override fun processResponse(jsonBody: JSONObject?, stringBody: String?, context: Context) {
        //Handle Pull Notifications response
        if (config.isAnalyticsOnly) {
            logger.verbose(
                config.accountId,
                "CleverTap instance is configured to analytics only, not processing push amp response"
            )
            return
        }
        try {
            if (jsonBody?.has("pushamp_notifs") != true) {
                return
            }
            logger.verbose(config.accountId, "Processing pushamp messages...")
            val pushAmpObject = jsonBody.getJSONObject("pushamp_notifs")
            val pushNotifications = pushAmpObject.getJSONArray("list")
            if (pushNotifications.length() > 0) {
                logger.verbose(config.accountId, "Handling Push payload locally")
                handlePushNotificationsInResponse(pushNotifications)
            }
            if (pushAmpObject.has("pf")) {
                try {
                    val frequency = pushAmpObject.getInt("pf")
                    checkNotNull(controllerManager.pushProviders).updatePingFrequencyIfNeeded(context, frequency)
                } catch (t: Throwable) {
                    logger.verbose("Error handling ping frequency in response : ${t.message}")
                }
            }
            if (pushAmpObject.optBoolean("ack", false)) {
                logger.verbose("Received ACK - true")
                val pushIds = baseDatabaseManager.loadDBAdapter(context).fetchPushNotificationIds()
                val rtlArray = pushIdsToJSONArray(pushIds)
                val rtlStringArray = Array(rtlArray.length()) { rtlArray.getString(it) }
                logger.verbose("Updating RTL values...")
                baseDatabaseManager.loadDBAdapter(context).updatePushNotificationIds(rtlStringArray)
            } else {
                logger.verbose("Received ACK - false")
            }
        } catch (t: Throwable) {
            //Ignore
        }
    }

    //PN
    private fun handlePushNotificationsInResponse(pushNotifications: JSONArray) {
        try {
            for (i in 0 until pushNotifications.length()) {
                val pushBundle = Bundle()
                val pushObject = pushNotifications.getJSONObject(i)
                if (pushObject.has("wzrk_ttl")) {
                    pushBundle.putLong("wzrk_ttl", pushObject.getLong("wzrk_ttl"))
                }

                for (key in pushObject.keys()) {
                    pushBundle.putString(key, pushObject.getString(key))
                }
                if (!pushBundle.isEmpty && !baseDatabaseManager.loadDBAdapter(context)
                        .doesPushNotificationIdExist(pushObject.getString("wzrk_pid"))
                ) {
                    logger.verbose("Creating Push Notification locally")
                    val pushAmpListener = callbackManager.pushAmpListener
                    if (pushAmpListener != null) {
                        pushAmpListener.onPushAmpPayloadReceived(pushBundle)
                    } else {
                        PushNotificationHandler.getPushNotificationHandler()
                            .onMessageReceived(context, pushBundle, PushConstants.FCM.toString())
                    }
                } else {
                    logger.verbose(
                        config.accountId,
                        "Push Notification already shown, ignoring local notification :${pushObject.getString("wzrk_pid")}"
                    )
                }
            }
        } catch (e: JSONException) {
            logger.verbose(config.accountId, "Error parsing push notification JSON")
        }
    }
}
