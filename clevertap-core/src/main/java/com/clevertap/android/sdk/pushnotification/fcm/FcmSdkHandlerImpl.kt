package com.clevertap.android.sdk.pushnotification.fcm

import android.content.Context
import android.text.TextUtils
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG
import com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG
import com.clevertap.android.sdk.pushnotification.PushType
import com.clevertap.android.sdk.utils.PackageUtils.isGooglePlayServicesAvailable
import com.clevertap.android.sdk.utils.PackageUtils.isGooglePlayStoreAvailable
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

/**
 * implementation of [IFcmMessageHandler]
 */
internal open class FcmSdkHandlerImpl(
    private val listener: CTPushProviderListener,
    private val context: Context,
    private val config: CleverTapInstanceConfig
) : IFcmSdkHandler {

    @Suppress("unused")
    private var manifestInfo: ManifestInfo? = ManifestInfo.getInstance(context)

    override val pushType: PushType
        get() = FCM

    override val isAvailable: Boolean
        get() {
            try {
                if (!isGooglePlayServicesAvailable(context)) {
                    config.log(LOG_TAG, FCM_LOG_TAG + "Google Play services is currently unavailable.")
                    return false
                }

                val senderId = senderId
                if (TextUtils.isEmpty(senderId)) {
                    config
                        .log(LOG_TAG, FCM_LOG_TAG + "The FCM sender ID is not set. Unable to register for FCM.")
                    return false
                }
            } catch (t: Throwable) {
                config.log(LOG_TAG, FCM_LOG_TAG + "Unable to register with FCM.", t)
                return false
            }
            return true
        }

    override val isSupported: Boolean
        get() = isGooglePlayStoreAvailable(context)

    override fun requestToken() {
        try {
            config.log(LOG_TAG, FCM_LOG_TAG + "Requesting FCM token using googleservices.json")
            FirebaseMessaging
                .getInstance()
                .token
                .addOnCompleteListener(object : OnCompleteListener<String> {
                    override fun onComplete(task: Task<String>) {
                        if (!task.isSuccessful) {
                            config.log(
                                LOG_TAG,
                                FCM_LOG_TAG + "FCM token using googleservices.json failed",
                                task.exception
                            )
                            listener.onNewToken(null, pushType)
                            return
                        }
                        val token: String? = if (task.result != null) task.result else null
                        config.log(
                            LOG_TAG,
                            FCM_LOG_TAG + "FCM token using googleservices.json - " + token
                        )
                        listener.onNewToken(token, pushType)
                    }
                })
        } catch (t: Throwable) {
            config.log(LOG_TAG, FCM_LOG_TAG + "Error requesting FCM token", t)
            listener.onNewToken(null, pushType)
        }
    }

    open val senderId: String?
        get() {
            val app = FirebaseApp.getInstance()
            return app.options.gcmSenderId
        }

    fun setManifestInfo(manifestInfo: ManifestInfo?) {
        this.manifestInfo = manifestInfo
    }
}
