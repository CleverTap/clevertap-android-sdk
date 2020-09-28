package com.clevertap.android.xps;

import android.content.Context;
import androidx.annotation.RestrictTo;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;

@RestrictTo(value = RestrictTo.Scope.LIBRARY)
public interface IMiMessageHandler {

    boolean createNotification(Context context, MiPushMessage message);

    int onReceiveRegisterResult(Context context, MiPushCommandMessage miPushCommandMessage);

}