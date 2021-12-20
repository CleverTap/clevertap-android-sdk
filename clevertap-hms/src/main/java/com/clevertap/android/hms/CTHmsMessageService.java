package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.HMS_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;

import com.clevertap.android.sdk.Logger;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

/**
 * Clevertap's Implementation for Huawei Message service
 */
public class CTHmsMessageService extends HmsMessageService {

    private IHmsMessageHandler mHandler = new CTHmsMessageHandler(new HmsNotificationParser());

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Logger.i(LOG_TAG, HMS_LOG_TAG + "onMessageReceived is called");
        mHandler.createNotification(getApplicationContext(), remoteMessage);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Logger.i(LOG_TAG, HMS_LOG_TAG + "onNewToken is called " + token);
        mHandler.onNewToken(getApplicationContext(), token);
    }
}