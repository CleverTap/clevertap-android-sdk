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

import java.util.List;

import static com.clevertap.android.xps.XpsConstants.FAILED_WITH_EXCEPTION;
import static com.clevertap.android.xps.XpsConstants.INVALID_TOKEN;
import static com.clevertap.android.xps.XpsConstants.LOG_TAG;
import static com.clevertap.android.xps.XpsConstants.OTHER_COMMAND;
import static com.clevertap.android.xps.XpsConstants.TOKEN_FAILURE;
import static com.clevertap.android.xps.XpsConstants.TOKEN_SUCCESS;

public class XiaomiMessageHandler implements IMiMessageHandler {
    @Override
    public boolean createNotification(Context context, MiPushMessage message) {
        boolean isSuccess = false;
        if (message != null) {
            try {
                String ctData = message.getContent();
                Bundle extras = Utils.stringToBundle(ctData);
                CleverTapAPI.createNotification(context, extras);
                Log.e(LOG_TAG, "Creating Notification");
                isSuccess = true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error Creating Notification");
            }
        } else {
            Log.e(LOG_TAG, "Received message entity is null!");
        }
        return isSuccess;
    }

    @Override
    public @XpsConstants.CommandResult
    int onReceiveRegisterResult(Context context, MiPushCommandMessage miPushCommandMessage) {
        try {
            Log.i(LOG_TAG, "onReceiveRegisterResult() : Message: " + miPushCommandMessage);
            String command = miPushCommandMessage.getCommand();
            if (!MiPushClient.COMMAND_REGISTER.equals(command)) {
                Log.e(LOG_TAG, "onReceiveRegisterResult() : Received command is not register command.");
                return OTHER_COMMAND;
            }

            if (miPushCommandMessage.getResultCode() != ErrorCode.SUCCESS) {
                Log.e(LOG_TAG, "onReceiveRegisterResult() : Registration failed.");
                return TOKEN_FAILURE;
            }

            List<String> arguments = miPushCommandMessage.getCommandArguments();
            String token = arguments != null && arguments.size() > 0 ? arguments.get(0) : null;
            if (TextUtils.isEmpty(token)) {
                Log.e(LOG_TAG, "onReceiveRegisterResult() : Token is null or empty");
                return INVALID_TOKEN;
            }
            CleverTapAPI.tokenRefresh(context, token, PushConstants.PushType.XPS);
            return TOKEN_SUCCESS;
        } catch (Throwable t) {
            Log.e(LOG_TAG, "onReceiveRegisterResult() : Exception: ", t);
            return FAILED_WITH_EXCEPTION;
        }
    }
}