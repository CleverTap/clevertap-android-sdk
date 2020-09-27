package com.clevertap.android.hms;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

public class CTHmsMessageService extends HmsMessageService {

    private IHmsMessageHandler mHandler = new HmsMessageHandlerImpl();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        mHandler.createNotification(getApplicationContext(), remoteMessage);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        mHandler.onNewToken(getApplicationContext(), token);
    }
}