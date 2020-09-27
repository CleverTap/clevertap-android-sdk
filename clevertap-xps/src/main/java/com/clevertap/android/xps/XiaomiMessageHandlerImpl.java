package com.clevertap.android.xps;

import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAccountIdFromNotificationBundle;
import static com.clevertap.android.xps.XpsConstants.FAILED_WITH_EXCEPTION;
import static com.clevertap.android.xps.XpsConstants.INVALID_TOKEN;
import static com.clevertap.android.xps.XpsConstants.OTHER_COMMAND;
import static com.clevertap.android.xps.XpsConstants.TOKEN_FAILURE;
import static com.clevertap.android.xps.XpsConstants.TOKEN_SUCCESS;
import static com.clevertap.android.xps.XpsConstants.XIAOMI_LOG_TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.pushnotification.NotificationInfo;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import java.util.List;

class XiaomiMessageHandlerImpl implements IMiMessageHandler {

    @Override
    public boolean createNotification(Context context, MiPushMessage message) {
        boolean isSuccess = false;
        if (message != null) {
            try {
                String ctData = message.getContent();
                Bundle extras = Utils.stringToBundle(ctData);
                NotificationInfo info = CleverTapAPI.getNotificationInfo(extras);
                CleverTapAPI cleverTapAPI = CleverTapAPI
                        .getGlobalInstance(context, getAccountIdFromNotificationBundle(extras));
                if (info.fromCleverTap) {
                    CleverTapAPI.createNotification(context, extras);
                    if (cleverTapAPI != null) {
                        cleverTapAPI.config().log(LOG_TAG, XIAOMI_LOG_TAG + "Creating Notification");
                    } else {
                        Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "Creating Notification");
                    }
                    isSuccess = true;
                } else {
                    Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "Can't create outside Clevertap Notification");
                }
            } catch (Exception e) {
                e.printStackTrace();
                isSuccess = false;
                Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "Error Creating Notification");
            }
        } else {
            Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "Received message entity is null!");
        }
        return isSuccess;
    }

    @Override
    public @XpsConstants.CommandResult
    int onReceiveRegisterResult(Context context, MiPushCommandMessage miPushCommandMessage) {
        try {
            Logger.d(LOG_TAG, "onReceiveRegisterResult() : Message: " + miPushCommandMessage);
            String command = miPushCommandMessage.getCommand();
            if (!MiPushClient.COMMAND_REGISTER.equals(command)) {
                Logger.d(LOG_TAG, "onReceiveRegisterResult() : Received command is not register command.");
                return OTHER_COMMAND;
            }

            if (miPushCommandMessage.getResultCode() != ErrorCode.SUCCESS) {
                Logger.d(LOG_TAG, "onReceiveRegisterResult() : Registration failed.");
                return TOKEN_FAILURE;
            }

            List<String> arguments = miPushCommandMessage.getCommandArguments();
            String token = arguments != null && arguments.size() > 0 ? arguments.get(0) : null;
            if (TextUtils.isEmpty(token)) {
                Logger.d(LOG_TAG, "onReceiveRegisterResult() : Token is null or empty");
                return INVALID_TOKEN;
            }
            CleverTapAPI.tokenRefresh(context, token, PushConstants.PushType.XPS);
            return TOKEN_SUCCESS;
        } catch (Throwable t) {
            Logger.d(LOG_TAG, "onReceiveRegisterResult() : Exception: ", t);
            return FAILED_WITH_EXCEPTION;
        }
    }
}