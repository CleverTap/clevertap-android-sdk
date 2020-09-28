package com.clevertap.android.sdk.pushnotification.fcm;

import android.content.Context;
import androidx.annotation.RestrictTo;
import com.google.firebase.messaging.RemoteMessage;

@RestrictTo(value = RestrictTo.Scope.LIBRARY)
public interface IFcmMessageHandler {

    boolean onMessageReceived(final Context applicationContext, RemoteMessage message);

    boolean onNewToken(Context applicationContext, String token);

}