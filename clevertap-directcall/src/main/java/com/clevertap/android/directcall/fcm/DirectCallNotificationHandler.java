package com.clevertap.android.directcall.fcm;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.RestrictTo;

import com.clevertap.android.directcall.init.DirectCallAPI;
import com.clevertap.android.directcall.utils.Utils;
import com.clevertap.android.sdk.interfaces.NotificationHandler;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DirectCallNotificationHandler implements NotificationHandler {

    @Override
    public boolean onMessageReceived(Context applicationContext, Bundle message, String pushType) {
        try {
            DirectCallAPI.getLogger().verbose("Inside Direct Call notification handler");
            // initial setup
            String accountId = Utils.getAccountIdFromNotificationBundle(message);
            if(!Utils.getInstance().initCleverTapApiIfRequired(applicationContext, accountId)) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX,
                        "cleverTapApi instance is null, can't process the VoIP push, dropping the call!");
                return false;
            }
            DirectCallAPI.getInstance().handleFcmMessage(applicationContext, message);
        } catch (Throwable throwable) {
            DirectCallAPI.getLogger().verbose("Error parsing Direct Call Push payload", throwable);
        }
        return true;
    }

    @Override
    public boolean onNewToken(Context applicationContext, String token, String pushType) {
        return true;
    }
}
