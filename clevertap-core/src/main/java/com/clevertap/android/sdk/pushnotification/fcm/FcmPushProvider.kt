package com.clevertap.android.sdk.pushnotification.fcm

import android.annotation.SuppressLint
import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.pushnotification.CTPushProvider
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushType

/**
 * Clevertap's Firebase Plugin Ref: [CTPushProvider]
 *
 * Instantiated by reflection from [com.clevertap.android.sdk.pushnotification.PushConstants.CT_FIREBASE_PROVIDER_CLASS]
 * through the `(CTPushProviderListener, Context, CleverTapInstanceConfig)` constructor.
 */
@SuppressLint("unused")
internal open class FcmPushProvider @SuppressLint("unused") constructor(
    ctPushListener: CTPushProviderListener,
    context: Context,
    config: CleverTapInstanceConfig
) : CTPushProvider {

    private var handler: IFcmSdkHandler = FcmSdkHandlerImpl(ctPushListener, context, config)

    override val pushType: PushType
        get() = handler.pushType

    /**
     * App supports FCM
     *
     * @return boolean true if FCM services are available
     */
    override val isAvailable: Boolean
        get() = handler.isAvailable

    /**
     * Device supports FCM
     *
     * @return - true if FCM is supported in the platform
     */
    override val isSupported: Boolean
        get() = handler.isSupported

    override fun minSDKSupportVersionCode(): Int {
        return 0 // supporting FCM from base version
    }

    override fun requestToken() {
        handler.requestToken()
    }

    fun setHandler(handler: IFcmSdkHandler) {
        this.handler = handler
    }
}
