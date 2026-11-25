package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.clevertap.android.sdk.Constants.WZRK_ACCT_ID_KEY
import com.clevertap.android.sdk.Constants.WZRK_PUSH_ID
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM
import java.util.ArrayList

@RestrictTo(Scope.LIBRARY_GROUP)
object PushNotificationUtil {

    @JvmStatic
    fun getAccountIdFromNotificationBundle(message: Bundle?): String {
        val defaultValue = ""
        return if (message != null) message.getString(WZRK_ACCT_ID_KEY, defaultValue) else defaultValue
    }

    @JvmStatic
    fun getPushIdFromNotificationBundle(message: Bundle?): String {
        val defaultValue = ""
        return if (message != null) message.getString(WZRK_PUSH_ID, defaultValue) else defaultValue
    }

    /**
     * Returns the names of all push types
     *
     * @return list
     */
    @JvmStatic
    fun getDefaultPushTypes(): ArrayList<PushType> {
        val list = ArrayList<PushType>()
        list.add(FCM)
        return list
    }

    @JvmStatic
    fun buildPushNotificationRenderedListenerKey(accountId: String?, pushId: String?): String {
        return accountId + "_" + pushId
    }

}
