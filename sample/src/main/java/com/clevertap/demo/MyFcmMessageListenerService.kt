package com.clevertap.demo

import android.os.Looper
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.pushnotification.fcm.CTFcmMessageHandler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFcmMessageListenerService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        message.data["nh_source"] = "MyFcmMessageListenerService"
        var pushType = "fcm"
        if (pushType.equals("fcm")) {
            android.os.Handler(Looper.getMainLooper()).post {
                println("MyFcmMessageListenerService onMessageReceived createNotification on ${Thread.currentThread()}")
                CTFcmMessageHandler()
                    .createNotification(applicationContext, message)
            }
            //CTFcmMessageHandler().processPushAmp(applicationContext, message)
        } else if (pushType.equals("hps")) {
            //CTHmsMessageHandler().createNotification(applicationContext,message)
            //CTHmsMessageHandler().processPushAmp(applicationContext,message)
        } else if (pushType.equals("xps")) {
            //CTXiaomiMessageHandler().createNotification(applicationContext,message)
            //CTXiaomiMessageHandler().processPushAmp(applicationContext,message)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CleverTapAPI.getDefaultInstance(this)?.apply {
            pushFcmRegistrationId(token, true)
        }
    }
}