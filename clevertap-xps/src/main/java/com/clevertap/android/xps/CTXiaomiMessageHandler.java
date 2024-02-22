package com.clevertap.android.xps;

import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.PushType.XPS;
import static com.clevertap.android.xps.XpsConstants.FAILED_WITH_EXCEPTION;
import static com.clevertap.android.xps.XpsConstants.INVALID_TOKEN;
import static com.clevertap.android.xps.XpsConstants.OTHER_COMMAND;
import static com.clevertap.android.xps.XpsConstants.TOKEN_FAILURE;
import static com.clevertap.android.xps.XpsConstants.TOKEN_SUCCESS;
import static com.clevertap.android.xps.XpsConstants.XIAOMI_LOG_TAG;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.interfaces.INotificationParser;
import com.clevertap.android.sdk.interfaces.IPushAmpHandler;
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler;
import com.clevertap.android.sdk.pushnotification.fcm.IFcmMessageHandler;
import com.xiaomi.channel.commonutils.android.Region;
import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;

import java.util.List;

/**
 * implementation of {@link IFcmMessageHandler} and {@link IPushAmpHandler} for xiaomi push message
 */
public class CTXiaomiMessageHandler implements IMiMessageHandler, IPushAmpHandler<MiPushMessage> {

    private @NonNull
    final
    INotificationParser<MiPushMessage> mParser;

    public CTXiaomiMessageHandler() {
        this(new XiaomiNotificationParser());
    }

    CTXiaomiMessageHandler(@NonNull final INotificationParser<MiPushMessage> parser) {
        mParser = parser;
    }

    /**
     * {@inheritDoc}
     * <br><br>
     * Use this method if you have custom implementation of xiaomi push service and wants to create push-template
     * notification/non push-template notification using CleverTap
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: Starting from core v5.1.0, this method runs on the caller's thread. Make sure to call it
     * in onMessageReceive() of messaging service.
     * </p>
     */
    @Override
    public boolean createNotification(Context context, MiPushMessage message) {
        boolean isSuccess = false;
        Bundle messageBundle = mParser.toBundle(message);
        if (messageBundle != null) {
            try {
                isSuccess = PushNotificationHandler
                        .getPushNotificationHandler().onMessageReceived(context, messageBundle, XPS.toString());

            } catch (Throwable e) {
                e.printStackTrace();
                isSuccess = false;
                Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "Error Creating Notification", e);
            }
        }
        return isSuccess;
    }

    /**
     * {@inheritDoc}
     */
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
            String region = MiPushClient.getAppRegion(context);
            region =TextUtils.isEmpty(region)? Region.Global.name() : region;
            Logger.v("default CTXiaomiMessageHandler: onReceiveRegisterResult | MiPushClient.getAppRegion(context) returns region="+region);
            XPS.setServerRegion(region);
            PushNotificationHandler.getPushNotificationHandler().onNewToken(context, token, XPS.getType());

            return TOKEN_SUCCESS;
        } catch (Throwable t) {
            Logger.d(LOG_TAG, "onReceiveRegisterResult() : Exception: ", t);
            return FAILED_WITH_EXCEPTION;
        }
    }

    /**
     * {@inheritDoc}
     * <br><br>
     * Use this method if you are rendering notification by your own and wants to support your custom rendered
     * notification for Pull Notifications
     */
    @Override
    public void processPushAmp(final Context context, @NonNull final MiPushMessage message) {
        try {
            Bundle messageBundle = mParser.toBundle(message);
            if (messageBundle != null) {
                CleverTapAPI.processPushNotification(context, messageBundle);
            }
        } catch (Throwable t) {
            Logger.d(LOG_TAG, XIAOMI_LOG_TAG + "Error processing push amp", t);
        }
    }
}