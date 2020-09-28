package com.clevertap.android.sdk.pushnotification;

import static com.clevertap.android.sdk.Constants.WZRK_ACCT_ID_KEY;

import android.os.Bundle;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import java.util.ArrayList;

@RestrictTo(Scope.LIBRARY_GROUP)
public class PushNotificationUtil {

    public static String getAccountIdFromNotificationBundle(Bundle message) {
        String defaultValue = "";
        return message != null ? message.getString(WZRK_ACCT_ID_KEY, defaultValue) : defaultValue;
    }

    private PushNotificationUtil() {

    }

    /**
     * Returns the names of all push types
     *
     * @return list
     */
    public static ArrayList<String> getAll() {
        ArrayList<String> list = new ArrayList<>();
        for (PushType pushType : PushType.values()) {
            list.add(pushType.name());
        }
        return list;
    }

    public static PushType[] getPushTypes(ArrayList<String> types) {
        PushType[] pushTypes = new PushType[0];
        if (types != null && !types.isEmpty()) {
            pushTypes = new PushType[types.size()];
            for (int i = 0; i < types.size(); i++) {
                pushTypes[i] = PushType.valueOf(types.get(i));
            }
        }
        return pushTypes;
    }

}