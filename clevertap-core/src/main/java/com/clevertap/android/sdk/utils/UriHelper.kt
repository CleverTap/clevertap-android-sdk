package com.clevertap.android.sdk.utils

import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import org.json.JSONObject
import java.net.URLDecoder

internal object UriHelper {

    fun getAllKeyValuePairs(url: String?, encodeValues: Boolean): Bundle {
        val customPairs = Bundle()
        if (url == null) {
            return customPairs
        }
        try {
            val sanitizer = UrlQuerySanitizer().apply {
                allowUnregisteredParamaters = true
                unregisteredParameterValueSanitizer = UrlQuerySanitizer.getAllButNulLegal()
                parseUrl(url)
            }
            for (key in sanitizer.parameterSet) {
                val value = getValueForKey(key, sanitizer, false) ?: continue
                // Don't encode wzrk_c2a - as the URL must be preserved
                if (encodeValues || key == "wzrk_c2a") {
                    // Already encoded
                    customPairs.putString(key, value)
                } else {
                    customPairs.putString(key, URLDecoder.decode(value, "UTF-8"))
                }
            }
        } catch (ignore: Throwable) {
            // Won't happen
        }
        return customPairs
    }

    fun getUrchinFromUri(uri: Uri?): JSONObject {
        val referrer = JSONObject()
        try {
            val sanitizer = UrlQuerySanitizer().apply {
                allowUnregisteredParamaters = true
                // a null uri fails here and yields the empty referrer (caught below), as before
                parseUrl(checkNotNull(uri).toString())
            }

            // Don't care for null values - they won't be added anyway
            referrer.put("us", getUtmOrWzrkValue("source", sanitizer))
            referrer.put("um", getUtmOrWzrkValue("medium", sanitizer))
            referrer.put("uc", getUtmOrWzrkValue("campaign", sanitizer))

            val wm = getWzrkValueForKey("medium", sanitizer)
            if (wm != null && wm.matches("^email$|^social$|^search$".toRegex())) {
                referrer.put("wm", wm)
            }

            Logger.d("Referrer data: ${referrer.toString(4)}")
        } catch (ignore: Throwable) {
            // Won't happen
        }
        return referrer
    }

    // Give preference to utm_*, else, try to look for wzrk_*
    private fun getUtmOrWzrkValue(utmKey: String, sanitizer: UrlQuerySanitizer): String? =
        getUtmValueForKey(utmKey, sanitizer) ?: getWzrkValueForKey(utmKey, sanitizer)

    private fun getUtmValueForKey(key: String, sanitizer: UrlQuerySanitizer): String? =
        getValueForKey("utm_$key", sanitizer, true)

    private fun getWzrkValueForKey(key: String, sanitizer: UrlQuerySanitizer): String? =
        getValueForKey("wzrk_$key", sanitizer, true)

    private fun getValueForKey(key: String, sanitizer: UrlQuerySanitizer, truncate: Boolean): String? = try {
        val value = sanitizer.getValue(key)
        if (value != null && truncate) value.take(Constants.MAX_KEY_LENGTH) else value
    } catch (t: Throwable) {
        Logger.v("Couldn't parse the URI", t)
        null
    }
}
