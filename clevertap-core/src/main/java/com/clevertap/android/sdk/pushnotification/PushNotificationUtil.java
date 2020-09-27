package com.clevertap.android.sdk.pushnotification;

import static com.clevertap.android.sdk.Constants.WZRK_ACCT_ID_KEY;

import android.os.Bundle;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

@RestrictTo(Scope.LIBRARY_GROUP)
public class PushNotificationUtil {

    public static String getAccountIdFromNotificationBundle(Bundle message) {
        String defaultValue = "";
        return message != null ? message.getString(WZRK_ACCT_ID_KEY, defaultValue) : defaultValue;
    }

    private PushNotificationUtil() {

    }
}