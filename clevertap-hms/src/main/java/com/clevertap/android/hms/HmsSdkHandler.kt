package com.clevertap.android.hms

import android.content.Context
import android.text.TextUtils
import com.clevertap.android.hms.HmsConstants.Companion.APP_ID_KEY
import com.clevertap.android.hms.HmsConstants.Companion.HCM_SCOPE
import com.clevertap.android.hms.HmsConstants.Companion.HMS_LOG_TAG
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.AGConnectOptions
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.HuaweiApiAvailability
import org.jetbrains.annotations.TestOnly
import androidx.annotation.RestrictTo

/**
 * Implementation of [IHmsSdkHandler]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class HmsSdkHandler(
    context: Context,
    private val mConfig: CleverTapInstanceConfig?
) : IHmsSdkHandler {

    private val context: Context = context.applicationContext

    @Volatile
    private var options: AGConnectOptions? = null

    @TestOnly
    constructor(context: Context, config: CleverTapInstanceConfig?, op: AGConnectOptions?) : this(context, config) {
        options = op
    }

    @Synchronized
    private fun initializeOptions() {
        if (options != null) {
            return
        }
        try {
            // Try existing AGConnect instance first
            val instance: AGConnectInstance? = AGConnectInstance.getInstance()
            if (instance != null && instance.options != null) {
                options = instance.options
                return
            }
        } catch (e: Exception) {
            mConfig!!.log(LOG_TAG, HMS_LOG_TAG + "Failed to initialize AGConnect options: " + e.message)
        }

        // Fallback to building new options
        try {
            options = AGConnectOptionsBuilder().build(context)
        } catch (e: Exception) {
            mConfig!!.log(LOG_TAG, HMS_LOG_TAG + "Failed to build new AGConnect options: " + e.message)
        }
    }

    override fun appId(): String? {
        if (options == null) {
            initializeOptions()
        }

        if (options == null) {
            return null
        }

        return try {
            options!!.getString(APP_ID_KEY)
        } catch (t: Throwable) {
            mConfig!!.log(LOG_TAG, HMS_LOG_TAG + "Failed to get HMS app ID: " + t.message)
            null
        }
    }

    override val isAvailable: Boolean
        get() = !TextUtils.isEmpty(appId())

    override val isSupported: Boolean
        get() = try {
            HuaweiApiAvailability.getInstance().isHuaweiMobileNoticeAvailable(context) == 0
        } catch (e: Throwable) {
            mConfig!!.log(LOG_TAG, HMS_LOG_TAG + "HMS is supported check failed.")
            false
        }

    override fun onNewToken(): String? {
        var token: String? = null
        try {
            val appId = appId()
            if (!TextUtils.isEmpty(appId)) {
                token = HmsInstanceId.getInstance(context).getToken(appId, HCM_SCOPE)
            }
        } catch (t: Throwable) {
            mConfig!!.log(LOG_TAG, HMS_LOG_TAG + "Error requesting HMS token", t)
        }
        return token
    }
}
