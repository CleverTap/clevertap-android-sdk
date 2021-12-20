package com.clevertap.demo

import com.clevertap.android.sdk.pushnotification.fcm.CTFcmMessageHandler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFcmMessageListenerService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        var pushType = "fcm"
        if (pushType.equals("fcm")) {
            CTFcmMessageHandler()
                .createNotification(applicationContext, message)
            //CTFcmMessageHandler().processPushAmp(applicationContext, message)
        } else if (pushType.equals("hps")) {
            //CTHmsMessageHandler().createNotification(applicationContext,message)
            //CTHmsMessageHandler().processPushAmp(applicationContext,message)
        } else if (pushType.equals("xps")) {
            //CTXiaomiMessageHandler().createNotification(applicationContext,message)
            //CTXiaomiMessageHandler().processPushAmp(applicationContext,message)
        }
    }
}