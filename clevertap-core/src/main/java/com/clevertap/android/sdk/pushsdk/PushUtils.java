package com.clevertap.android.sdk.pushsdk;

import android.os.Bundle;
import org.json.JSONObject;

public class PushUtils {

    public static  String  bundleToString(Bundle bundle){
        String bundleAsString = "";
        try {
            JSONObject j = new JSONObject();
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                j.put(key,value);
            }
            bundleAsString= j.toString();
            return bundleAsString;
        }
        catch (Throwable t){
            t.printStackTrace();
            return bundleAsString;
        }
    }

}
