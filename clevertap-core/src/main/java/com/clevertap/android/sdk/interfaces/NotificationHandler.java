package com.clevertap.android.sdk.interfaces;

import android.content.Context;
import android.os.Bundle;

import com.clevertap.android.sdk.pushnotification.PushType;

/**
 * Handles push messages and push tokens delivered by a messaging provider (FCM, HMS, ...).
 * <p>
 * The SDK's own implementation is {@link com.clevertap.android.sdk.pushnotification.PushNotificationHandler}.
 * A handler registered with {@code CleverTapAPI.setNotificationHandler} (for example
 * {@code PushTemplateNotificationHandler}) receives Push Template payloads, and one registered with
 * {@code CleverTapAPI.setSignedCallNotificationHandler} receives Signed Call payloads; core renders
 * everything else. Methods are called on the thread that delivers the message or token, for example
 * the messaging service's worker thread.
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameters as platform types, so wrapper SDKs
 * and apps can override them with nullable or non-null types. Do not convert this file to Kotlin.
 */
public interface NotificationHandler {

    /**
     * Handles a received push message.
     *
     * @param applicationContext - application context
     * @param message            - notification message from cloud messaging owners
     * @param pushType           - a label naming the provider that delivered the message (used in logs)
     * @return {@code true} if the message was handled (it came from CleverTap), {@code false} otherwise
     */
    boolean onMessageReceived(final Context applicationContext, Bundle message, final String pushType);

    /**
     * Handles a new push token issued by the provider.
     *
     * @param applicationContext - application context
     * @param token              - token received from cloud messaging owners
     * @param pushType           - the provider that issued the token
     * @return {@code true} if the token was handled
     */
    boolean onNewToken(Context applicationContext, String token, final PushType pushType);
}
