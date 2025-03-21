package com.clevertap.android.sdk

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clevertap.android.sdk.Constants.NOTIFICATION_PERMISSION_REQUEST_CODE
import com.clevertap.android.sdk.inapp.AlertDialogPromptForSettings
import java.lang.ref.WeakReference

internal class PushPermissionHandler(
    private val config: CleverTapInstanceConfig,
    private val ctListeners: List<PushPermissionResponseListener?>?,
    callback: PushPermissionResultCallback? = null
) {

    interface PushPermissionResultCallback {
        fun onPushPermissionResult(isGranted: Boolean)
    }

    interface PushPermissionFlowCallback {
        fun onRequestPermission()
        fun onShowFallback()
    }

    companion object {
        internal const val ANDROID_PERMISSION_STRING = "android.permission.POST_NOTIFICATIONS"

        fun isPushPermissionGranted(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(context, ANDROID_PERMISSION_STRING) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private val pushPermissionCallback = WeakReference(callback)
    private var isFromNotificationSettingsActivity: Boolean = false

    fun requestPermission(
        context: Context,
        fallbackEnabled: Boolean,
        flowCallback: PushPermissionFlowCallback
    ) {
        if (isPushPermissionGranted(context)) {
            notifyListeners(isPermissionGranted = true)
            return
        }

        val isFirstTimeRequest =
            CTPreferenceCache.getInstance(context, config).isFirstTimeRequest()

        val currentActivity = CoreMetaData.getCurrentActivity()
        if (currentActivity == null) {
            config.logger.debug(
                "CurrentActivity reference is null. SDK can't prompt the user with Notification Permission! Ensure the following things:\n" +
                        "1. Calling ActivityLifecycleCallback.register(this) in your custom application class before super.onCreate().\n" +
                        "   Alternatively, register CleverTap SDK's Application class in the manifest using com.clevertap.android.sdk.Application.\n" +
                        "2. Ensure that the promptPushPrimer() API is called from the onResume() lifecycle method, not onCreate()."
            )
            return
        }
        val shouldShowRequestPermissionRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                currentActivity,
                ANDROID_PERMISSION_STRING
            )

        if (!isFirstTimeRequest && shouldShowRequestPermissionRationale) {
            if (fallbackEnabled) {
                flowCallback.onShowFallback()
            } else {
                notifyListeners(isPermissionGranted = false)
            }
            return
        }

        flowCallback.onRequestPermission()
    }

    fun requestPermission(activity: Activity, fallbackToSettings: Boolean) {
        requestPermission(
            activity,
            fallbackToSettings,
            object : PushPermissionFlowCallback {
                override fun onRequestPermission() {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(ANDROID_PERMISSION_STRING),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }

                override fun onShowFallback() {
                    showFallbackAlertDialog(activity)
                }
            })
    }


    fun showFallbackAlertDialog(activity: Activity) {
        AlertDialogPromptForSettings.show(activity,
            onAccept = {
                Utils.navigateToAndroidSettingsForNotifications(activity)
                isFromNotificationSettingsActivity = true
            },
            onDecline = {
                notifyListeners(false)
            })
    }

    fun onActivityResume(activity: Activity) {
        if (isFromNotificationSettingsActivity) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notifyListeners(isPushPermissionGranted(activity))
            }
        }
    }

    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        CTPreferenceCache.getInstance(activity, config).setFirstTimeRequest(false)
        CTPreferenceCache.updateCacheToDisk(activity, config)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val isGranted = PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()
            notifyListeners(isGranted)
        }
    }

    private fun notifyListeners(isPermissionGranted: Boolean) {
        pushPermissionCallback.get()?.onPushPermissionResult(isPermissionGranted)
        if (ctListeners != null) {
            for (listener in ctListeners) {
                listener?.onPushPermissionResponse(isPermissionGranted)
            }
        }
    }
}
