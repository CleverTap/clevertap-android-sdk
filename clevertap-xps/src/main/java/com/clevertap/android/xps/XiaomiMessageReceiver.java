package com.clevertap.android.xps;

import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.xps.XpsConstants.XIAOMI_LOG_TAG;

import android.content.Context;
import com.clevertap.android.sdk.Logger;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

/**
 * Clevertap's Implementation for Xiaomi Message Receiver
 */
public class XiaomiMessageReceiver extends PushMessageReceiver {

    private IMiMessageHandler handler = new CTXiaomiMessageHandler(new XiaomiNotificationParser());

    @Override
    public void onNotificationMessageArrived(Context context, MiPushMessage miPushMessage) {
        super.onNotificationMessageArrived(context, miPushMessage);
        Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "onNotificationMessageArrived is called");
        handler.createNotification(context, miPushMessage);
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
}