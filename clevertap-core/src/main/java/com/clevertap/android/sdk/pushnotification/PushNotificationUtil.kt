package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.clevertap.android.sdk.Constants.WZRK_ACCT_ID_KEY
import com.clevertap.android.sdk.Constants.WZRK_PUSH_ID
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM

/**
 * `@JvmStatic` is kept on purpose: this is a cross-module (`LIBRARY_GROUP`) type, and separately
 * published artifacts (push templates, hms) built against the former Java class call it statically.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
object PushNotificationUtil {

    @JvmStatic
    fun getAccountIdFromNotificationBundle(message: Bundle?): String =
        message?.getString(WZRK_ACCT_ID_KEY, "") ?: ""

    @JvmStatic
    fun getPushIdFromNotificationBundle(message: Bundle?): String =
        message?.getString(WZRK_PUSH_ID, "") ?: ""

    /**
     * Returns the names of all push types
     *
     * @return list
     */
    @JvmStatic
    fun getDefaultPushTypes(): ArrayList<PushType> = arrayListOf(FCM)

    @JvmStatic
    fun buildPushNotificationRenderedListenerKey(accountId: String?, pushId: String?): String =
        "${accountId}_$pushId"
}
