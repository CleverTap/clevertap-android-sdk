package com.clevertap.android.hms;

import static com.clevertap.android.hms.HmsConstants.LOG_TAG;

import android.util.Log;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

public class CTHmsMessageService extends HmsMessageService {

    private IHmsMessageHandler mHandler = new HmsMessageHandler();


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(LOG_TAG, "onMessageReceived: ");
        mHandler.createNotification(getApplicationContext(), remoteMessage);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(LOG_TAG, "onNewToken: " + token);
        mHandler.onNewToken(getApplicationContext(), token);
    }
}