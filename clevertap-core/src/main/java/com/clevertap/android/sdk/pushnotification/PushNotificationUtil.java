package com.clevertap.android.sdk.pushnotification;

import static com.clevertap.android.sdk.Constants.WZRK_ACCT_ID_KEY;
import static com.clevertap.android.sdk.Constants.WZRK_PUSH_ID;
import static com.clevertap.android.sdk.pushnotification.PushType.FCM;

import android.os.Bundle;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.ArrayList;

@RestrictTo(Scope.LIBRARY_GROUP)
public class PushNotificationUtil {

    public static String getAccountIdFromNotificationBundle(Bundle message) {
        String defaultValue = "";
        return message != null ? message.getString(WZRK_ACCT_ID_KEY, defaultValue) : defaultValue;
    }

    public static String getPushIdFromNotificationBundle(Bundle message) {
        String defaultValue = "";
        return message != null ? message.getString(WZRK_PUSH_ID, defaultValue) : defaultValue;
    }

    /**
     * Returns the names of all push types
     *
     * @return list
     */
    public static ArrayList<String> getAll() {
        ArrayList<String> list = new ArrayList<>();
        list.add(FCM.name());
        return list;
    }

    private PushNotificationUtil() {

    }

    public static String buildPushNotificationRenderedListenerKey(String accountId, String pushId){
        return accountId+"_"+pushId;
    }

}