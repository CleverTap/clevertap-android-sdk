package com.clevertap.android.hms

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RestrictTo
import com.clevertap.android.hms.HmsConstants.Companion.HPS
import com.clevertap.android.hms.HmsConstants.Companion.MIN_CT_ANDROID_SDK_VERSION
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.pushnotification.CTPushProvider
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushType

/**
 * Clevertap's Huawei Plugin Ref: [CTPushProvider]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class HmsPushProvider @SuppressLint("unused") constructor(
    private val ctPushListener: CTPushProviderListener,
    context: Context,
    config: CleverTapInstanceConfig?
) : CTPushProvider {

    private var hmsSdkHandler: IHmsSdkHandler = HmsSdkHandler(context, config)

    override val pushType: PushType
        get() = HPS

    override val isAvailable: Boolean
        get() = hmsSdkHandler.isAvailable

    override val isSupported: Boolean
        get() = hmsSdkHandler.isSupported

    override fun minSDKSupportVersionCode(): Int {
        return MIN_CT_ANDROID_SDK_VERSION
    }

    override fun requestToken() {
        ctPushListener.onNewToken(hmsSdkHandler.onNewToken(), pushType)
    }

    fun setHmsSdkHandler(hmsSdkHandler: IHmsSdkHandler) {
        this.hmsSdkHandler = hmsSdkHandler
    }
}
