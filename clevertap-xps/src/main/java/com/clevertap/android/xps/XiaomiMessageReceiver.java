package com.clevertap.android.xps;

import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.xps.XpsConstants.XIAOMI_LOG_TAG;

import android.content.Context;
import android.os.Bundle;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;
import java.util.Objects;

/**
 * Clevertap's Implementation for Xiaomi Message Receiver
 */
public class XiaomiMessageReceiver extends PushMessageReceiver {
    private final XiaomiNotificationParser xpsParser = new XiaomiNotificationParser();
    private IMiMessageHandler handler = new CTXiaomiMessageHandler(xpsParser);

    @Override
    public void onNotificationMessageArrived(Context context, MiPushMessage miPushMessage) {
        super.onNotificationMessageArrived(context, miPushMessage);
        Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "onNotificationMessageArrived is called");
        pushNotificationViewedEvent(context,miPushMessage,xpsParser);
    }

    @Override
    public void onReceivePassThroughMessage(Context context, MiPushMessage miPushMessage) {
        Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "onReceivePassThroughMessage is called");
        handler.createNotification(context, miPushMessage);
    }

    @Override
    public void onReceiveRegisterResult(Context context, MiPushCommandMessage miPushCommandMessage) {
        super.onReceiveRegisterResult(context, miPushCommandMessage);
        Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "onReceiveRegisterResult is called");
        handler.onReceiveRegisterResult(context, miPushCommandMessage);
    }

    void setHandler(IMiMessageHandler handler) {
        this.handler = handler;
    }

    private void pushNotificationViewedEvent(Context context, MiPushMessage miPushMessage, XiaomiNotificationParser xpsParser) {
        try {
            Objects.requireNonNull(miPushMessage);
            Objects.requireNonNull(xpsParser);
            Bundle data = xpsParser.toBundle(miPushMessage);

            Objects.requireNonNull(data);
            String acc = PushNotificationUtil.getAccountIdFromNotificationBundle(data);

            Objects.requireNonNull(context);
            Objects.requireNonNull(acc);
            CleverTapAPI ct = CleverTapAPI.getGlobalInstance(context, acc);

            Objects.requireNonNull(ct);
            ct.pushNotificationViewedEvent(data);
        }
        catch (Throwable t){
            Logger.i("XiaomiMessageReceiver|onNotificationMessageArrived : something went wrong",t);
        }
    }

}