package com.clevertap.android.sdk.interfaces;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameters carry
 * androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin override
 * must declare the same nullability. Do not convert this file to Kotlin.
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
    boolean onMessageReceived(@Nullable final Context applicationContext, @NonNull Bundle message,
            @Nullable final String pushType);

    /**
     * Handles a new push token issued by the provider.
     *
     * @param applicationContext - application context
     * @param token              - token received from cloud messaging owners
     * @param pushType           - the provider that issued the token
     * @return {@code true} if the token was handled
     */
    boolean onNewToken(@Nullable Context applicationContext, @Nullable String token,
            @Nullable final PushType pushType);
}
