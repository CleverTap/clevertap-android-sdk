package com.clevertap.android.sdk;

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import java.net.URLDecoder;
import java.util.Set;
import org.json.JSONObject;

final class UriHelper {

    static Bundle getAllKeyValuePairs(String url, boolean encodeValues) {
        if (url == null) {
            return new Bundle();
        }
        Bundle customPairs = new Bundle();
        try {
            UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
            sanitizer.setAllowUnregisteredParamaters(true);
            sanitizer.setUnregisteredParameterValueSanitizer(UrlQuerySanitizer.getAllButNulLegal());
            sanitizer.parseUrl(url);
            Set<String> queryParams = sanitizer.getParameterSet();
            for (String key : queryParams) {
                String value = getValueForKey(key, sanitizer, false);
                if (value != null) {
                    // Don't encode wzrk_c2a - as the URL must be preserved
                    if (encodeValues || key.equals("wzrk_c2a")) {
                        // Already encoded
                        customPairs.putString(key, value);
                    } else {
                        customPairs.putString(key, URLDecoder.decode(value, "UTF-8"));
                    }
                }
            }
        } catch (Throwable ignore) {
            // Won't happen
        }
        return customPairs;
    }

    /*package*/
    static JSONObject getUrchinFromUri(Uri uri) {
        JSONObject referrer = new JSONObject();
        try {

            UrlQuerySanitizer sanitizer = new UrlQuerySanitizer();
            sanitizer.setAllowUnregisteredParamaters(true);
            sanitizer.parseUrl(uri.toString());

            // Don't care for null values - they won't be added anyway
            String source = getUtmOrWzrkValue("source", sanitizer);
            String medium = getUtmOrWzrkValue("medium", sanitizer);
            String campaign = getUtmOrWzrkValue("campaign", sanitizer);

            referrer.put("us", source);
            referrer.put("um", medium);
            referrer.put("uc", campaign);

            String wm = getWzrkValueForKey("medium", sanitizer);
            if (wm != null && wm.matches("^email$|^social$|^search$")) {
                referrer.put("wm", wm);
            }

            Logger.d("Referrer data: " + referrer.toString(4));
        } catch (Throwable ignore) {
            // Won't happen
        }
        return referrer;
    }

    private static String getUtmOrWzrkValue(String utmKey, UrlQuerySanitizer sanitizer) {
        // Give preference to utm_*, else, try to look for wzrk_*
        String value;
        if ((value = getUtmValueForKey(utmKey, sanitizer)) != null
                || (value = getWzrkValueForKey(utmKey, sanitizer)) != null) {
            return value;
        } else {
            return null;
        }
    }

    private static String getUtmValueForKey(String key, UrlQuerySanitizer sanitizer) {
        key = "utm_" + key;
        return getValueForKey(key, sanitizer, true);
    }

    private static String getValueForKey(String key, UrlQuerySanitizer sanitizer, boolean truncate) {
        if (key == null || sanitizer == null) {
            return null;
        }
        try {
            String value = sanitizer.getValue(key);

            if (value == null) {
                return null;
            }
            if (truncate && value.length() > Constants.MAX_KEY_LENGTH) {
                return value.substring(0, Constants.MAX_KEY_LENGTH);
            } else {
                return value;
            }
        } catch (Throwable t) {
            Logger.v("Couldn't parse the URI", t);
            return null;
        }
    }

    private static String getWzrkValueForKey(String key, UrlQuerySanitizer sanitizer) {
        key = "wzrk_" + key;
        return getValueForKey(key, sanitizer, true);
    }

}
