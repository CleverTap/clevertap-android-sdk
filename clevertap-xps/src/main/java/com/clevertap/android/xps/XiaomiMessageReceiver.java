package com.clevertap.android.xps;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

import org.json.JSONException;

import java.util.List;

public class XiaomiMessageReceiver extends PushMessageReceiver {
    private static final String TAG = "XiaomiMessageReceiver";

    @Override
    public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
        Log.i(TAG, "onReceivePassThroughMessage is called");
        createNotification(context, message);
    }

    private void createNotification(Context context, MiPushMessage message) {
        if (message == null) {
            Log.e(TAG, "Received message entity is null!");
            return;
        }
        try {
            String ctData = message.getContent();
            Bundle extras = Utils.stringToBundle(ctData);
            CleverTapAPI.createNotification(context, extras);
            Log.e(TAG, "Creating Notification");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Error Creating Notification");
        }
    }

    @Override
    public void onNotificationMessageArrived(Context context, MiPushMessage miPushMessage) {
        super.onNotificationMessageArrived(context, miPushMessage);
        Log.i(TAG, "onNotificationMessageArrived is called");
        createNotification(context, miPushMessage);
    }

    @Override
    public void onReceiveRegisterResult(Context context, MiPushCommandMessage miPushCommandMessage) {
        super.onReceiveRegisterResult(context, miPushCommandMessage);
        try {
            Log.i(TAG, "onReceiveRegisterResult() : Message: " + miPushCommandMessage);
            String command = miPushCommandMessage.getCommand();
            if (!MiPushClient.COMMAND_REGISTER.equals(command)) {
                Log.e(TAG, "onReceiveRegisterResult() : Received command is not register command.");
                return;
            }

            if (miPushCommandMessage.getResultCode() != ErrorCode.SUCCESS) {
                Log.e(TAG, "onReceiveRegisterResult() : Registration failed.");
                return;
            }

            List<String> arguments = miPushCommandMessage.getCommandArguments();
            String token = arguments != null && arguments.size() > 0 ? arguments.get(0) : null;
            if (TextUtils.isEmpty(token)) {
                Log.e(TAG, "onReceiveRegisterResult() : Token is null or empty");
                return;
            }
            CleverTapAPI.tokenRefresh(context, token, PushConstants.PushType.XPS);

        } catch (Throwable t) {
            Log.e(TAG, "onReceiveRegisterResult() : Exception: ", t);
        }
    }
}