package com.clevertap.android.sdk

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clevertap.android.sdk.Constants.NOTIFICATION_PERMISSION_REQUEST_CODE
import java.lang.ref.WeakReference

internal class PushPermissionHandler @JvmOverloads constructor(
    private val config: CleverTapInstanceConfig,
    private val ctListeners: List<PushPermissionResponseListener?>?,
    callback: PushPermissionResultCallback? = null,
    private val cacheProvider: (Context) -> CTPreferenceCache = defaultCacheProvider(config),
    private val systemPermissionInterface: SystemPushPermissionInterface = defaultSystemInterface()
) {

    interface PushPermissionResultCallback {
        fun onPushPermissionResult(isGranted: Boolean)
    }

    interface PushPermissionRequestCallback {
        fun onRequestPermission()
    }

    companion object {
        internal const val ANDROID_PERMISSION_STRING = "android.permission.POST_NOTIFICATIONS"

        private fun defaultCacheProvider(config: CleverTapInstanceConfig) =
            { context: Context ->
                CTPreferenceCache.getInstance(context, config)
            }

        private fun defaultSystemInterface(): SystemPushPermissionInterface =
            object : SystemPushPermissionInterface {
                override fun isPushPermissionGranted(context: Context): Boolean {
                    return ContextCompat.checkSelfPermission(context, ANDROID_PERMISSION_STRING) ==
                            PackageManager.PERMISSION_GRANTED
                }

                override fun requestPushPermission(activity: Activity) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(ANDROID_PERMISSION_STRING),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }

                override fun navigateToNotificationSettings(activity: Activity) {
                    Utils.navigateToAndroidSettingsForNotifications(activity)
                }

                override fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
                    return ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        ANDROID_PERMISSION_STRING
                    )
                }
            }
    }

    private val pushPermissionCallback = WeakReference(callback)
    private var isFromNotificationSettingsActivity: Boolean = false

    fun isPushPermissionGranted(context: Context): Boolean {
        return systemPermissionInterface.isPushPermissionGranted(context)
    }

    /**
     * Attempt to request a push permission. Checks if the permission is already given and does not
     * initiate the flow in this case. When the result of the permission check is known without
     * initiation of the permission flow - notifies the attached [ctListeners] and the
     * [PushPermissionResultCallback].
     *
     * @return Whether the permission flow was initiated.
     */
    fun requestPermission(
        activity: Activity,
        fallbackToSettings: Boolean,
        requestCallback: PushPermissionRequestCallback
    ): Boolean {
        if (isPushPermissionGranted(activity)) {
            notifyListeners(isPermissionGranted = true)
            return false
        }

        val isFirstTimeRequest = cacheProvider(activity).isFirstTimeRequest()
        val showRationale = systemPermissionInterface.shouldShowRequestPermissionRationale(activity)

        if (!isFirstTimeRequest && showRationale) {
            if (fallbackToSettings) {
                isFromNotificationSettingsActivity = true
                systemPermissionInterface.navigateToNotificationSettings(activity)
                return true
            } else {
                notifyListeners(isPermissionGranted = false)
                return false
            }
        }

        requestCallback.onRequestPermission()
        return true
    }

    /**
     * Request a permission from an [Activity]. Starts the permission flow and notifies the [ctListeners]
     * and the [PushPermissionResultCallback] when the flow completes. This method should only be
     * called from an [Activity].[onActivityResume] and [onRequestPermissionsResult] must also
     * be called within the corresponding [Activity] methods.
     */
    fun requestPermission(activity: Activity, fallbackToSettings: Boolean) {
        requestPermission(
            activity,
            fallbackToSettings,
            object : PushPermissionRequestCallback {
                override fun onRequestPermission() {
                    systemPermissionInterface.requestPushPermission(activity)
                }
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
        grantResults: IntArray
    ) {
        val ctCache = cacheProvider(activity)
        ctCache.setFirstTimeRequest(false)
        ctCache.updateCacheToDisk(activity, config)

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

    interface SystemPushPermissionInterface {
        fun isPushPermissionGranted(context: Context): Boolean
        fun requestPushPermission(activity: Activity)
        fun navigateToNotificationSettings(activity: Activity)
        fun shouldShowRequestPermissionRationale(activity: Activity): Boolean
    }
}
