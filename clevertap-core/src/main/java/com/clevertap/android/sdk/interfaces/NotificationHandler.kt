package com.clevertap.android.sdk.interfaces

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.pushnotification.PushType

/**
 * Handles push messages and push tokens delivered by a messaging provider (FCM, HMS, ...).
 *
 * The SDK's own implementation is [com.clevertap.android.sdk.pushnotification.PushNotificationHandler].
 * A handler registered with [com.clevertap.android.sdk.CleverTapAPI.setNotificationHandler] (for
 * example `PushTemplateNotificationHandler`) receives Push Template payloads, and one registered with
 * [com.clevertap.android.sdk.CleverTapAPI.setSignedCallNotificationHandler] receives Signed Call
 * payloads; core renders everything else. Methods are called on the thread that delivers the message or token,
 * for example the messaging service's worker thread.
 */
public interface NotificationHandler {

    /**
     * Handles a received push message.
     *
     * @param applicationContext - application context
     * @param message            - notification message from cloud messaging owners
     * @param pushType           - a label naming the provider that delivered the message (used in
     *                             logs)
     * @return `true` if the message was handled (it came from CleverTap), `false` otherwise
     */
    public fun onMessageReceived(applicationContext: Context?, message: Bundle?, pushType: String?): Boolean

    /**
     * Handles a new push token issued by the provider.
     *
     * @param applicationContext - application context
     * @param token              - token received from cloud messaging owners
     * @param pushType           - the provider that issued the token
     * @return `true` if the token was handled
     */
    public fun onNewToken(applicationContext: Context?, token: String?, pushType: PushType?): Boolean
}
