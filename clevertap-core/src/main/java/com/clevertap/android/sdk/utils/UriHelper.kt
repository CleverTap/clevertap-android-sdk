package com.clevertap.android.sdk.utils

import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import org.json.JSONObject
import java.net.URLDecoder

internal object UriHelper {

    @JvmStatic
    fun getAllKeyValuePairs(url: String?, encodeValues: Boolean): Bundle {
        if (url == null) {
            return Bundle()
        }
        val customPairs = Bundle()
        try {
            val sanitizer = UrlQuerySanitizer()
            sanitizer.allowUnregisteredParamaters = true
            sanitizer.unregisteredParameterValueSanitizer = UrlQuerySanitizer.getAllButNulLegal()
            sanitizer.parseUrl(url)
            val queryParams = sanitizer.parameterSet
            for (key in queryParams) {
                val value = getValueForKey(key, sanitizer, false)
                if (value != null) {
                    // Don't encode wzrk_c2a - as the URL must be preserved
                    if (encodeValues || key == "wzrk_c2a") {
                        // Already encoded
                        customPairs.putString(key, value)
                    } else {
                        customPairs.putString(key, URLDecoder.decode(value, "UTF-8"))
                    }
                }
            }
        } catch (ignore: Throwable) {
            // Won't happen
        }
        return customPairs
    }

    /*package*/
    @JvmStatic
    fun getUrchinFromUri(uri: Uri?): JSONObject {
        val referrer = JSONObject()
        try {
            val sanitizer = UrlQuerySanitizer()
            sanitizer.allowUnregisteredParamaters = true
            sanitizer.parseUrl(uri!!.toString())

            // Don't care for null values - they won't be added anyway
            val source = getUtmOrWzrkValue("source", sanitizer)
            val medium = getUtmOrWzrkValue("medium", sanitizer)
            val campaign = getUtmOrWzrkValue("campaign", sanitizer)

            referrer.put("us", source)
            referrer.put("um", medium)
            referrer.put("uc", campaign)

            val wm = getWzrkValueForKey("medium", sanitizer)
            if (wm != null && wm.matches("^email$|^social$|^search$".toRegex())) {
                referrer.put("wm", wm)
            }

            Logger.d("Referrer data: " + referrer.toString(4))
        } catch (ignore: Throwable) {
            // Won't happen
        }
        return referrer
    }

    private fun getUtmOrWzrkValue(utmKey: String, sanitizer: UrlQuerySanitizer): String? {
        // Give preference to utm_*, else, try to look for wzrk_*
        return getUtmValueForKey(utmKey, sanitizer) ?: getWzrkValueForKey(utmKey, sanitizer)
    }

    private fun getUtmValueForKey(key: String, sanitizer: UrlQuerySanitizer): String? {
        return getValueForKey("utm_$key", sanitizer, true)
    }

    private fun getValueForKey(key: String?, sanitizer: UrlQuerySanitizer?, truncate: Boolean): String? {
        if (key == null || sanitizer == null) {
            return null
        }
        try {
            val value = sanitizer.getValue(key) ?: return null

            return if (truncate && value.length > Constants.MAX_KEY_LENGTH) {
                value.substring(0, Constants.MAX_KEY_LENGTH)
            } else {
                value
            }
        } catch (t: Throwable) {
            Logger.v("Couldn't parse the URI", t)
            return null
        }
    }

    private fun getWzrkValueForKey(key: String, sanitizer: UrlQuerySanitizer): String? {
        return getValueForKey("wzrk_$key", sanitizer, true)
    }
}
