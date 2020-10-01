package com.clevertap.android.xps;

import android.content.Context;
import androidx.annotation.RestrictTo;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;

/**
 * interface to handle the xiaomi notification message receiver callbacks
 */
@RestrictTo(value = RestrictTo.Scope.LIBRARY)
public interface IMiMessageHandler {

    /**
     * @param context - application context
     * @param message - Xiaomi MiPushMessage
     * @return true if everything is fine & notification is rendered successfully
     */
    boolean createNotification(Context context, MiPushMessage message);

    /**
     * @param context              - application context
     * @param miPushCommandMessage - miCommand Message
     * @return message processed result code after processing, Ref {@link XpsConstants.CommandResult}
     */
    int onReceiveRegisterResult(Context context, MiPushCommandMessage miPushCommandMessage);

}