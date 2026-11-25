package com.clevertap.android.sdk.validation

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.InAppNotificationActivity
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.inbox.CTInboxActivity
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService
import com.clevertap.android.sdk.pushnotification.CTPushNotificationReceiver
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.sdk.pushnotification.PushProviders

internal object ManifestValidator {

    private const val ourApplicationClassName = "com.clevertap.android.sdk.Application"

    @JvmStatic
    fun validate(context: Context, deviceInfo: DeviceInfo, pushProviders: PushProviders) {
        if (!Utils.hasPermission(context, "android.permission.INTERNET")) {
            Logger.d("Missing Permission: android.permission.INTERNET")
        }
        checkSDKVersion(deviceInfo)
        validationApplicationLifecyleCallback(context)
        checkReceiversServices(context, pushProviders)
        if (!TextUtils.isEmpty(ManifestInfo.getInstance(context).fcmSenderId)) {
            Logger.i(
                "We have noticed that your app is using a custom FCM Sender ID, this feature will " +
                        "be DISCONTINUED from the next version of the CleverTap Android SDK. With the next release, " +
                        "CleverTap Android SDK will only fetch the token using the google-services.json." +
                        " Please reach out to CleverTap Support for any questions."
            )
        }
    }

    private fun checkApplicationClass(context: Context) {
        val appName = context.applicationInfo.className
        if (appName == null || appName.isEmpty()) {
            Logger.i("Unable to determine Application Class")
        } else if (appName == ourApplicationClassName) {
            Logger.i(
                "AndroidManifest.xml uses the CleverTap Application class, " +
                        "be sure you have properly added the CleverTap Account ID and Token to your AndroidManifest.xml, \n"
                        +
                        "or set them programmatically in the onCreate method of your custom application class prior to calling super.onCreate()"
            )
        } else {
            Logger.i("Application Class is $appName")
        }
    }

    private fun checkReceiversServices(context: Context, pushProviders: PushProviders) {
        try {
            validateReceiverInManifest(
                context.applicationContext as Application,
                CTPushNotificationReceiver::class.java.name
            )
            validateServiceInManifest(
                context.applicationContext as Application,
                CTNotificationIntentService::class.java.name
            )
            validateActivityInManifest(
                context.applicationContext as Application,
                InAppNotificationActivity::class.java
            )
            validateActivityInManifest(
                context.applicationContext as Application,
                CTInboxActivity::class.java
            )
            validateReceiverInManifest(
                context.applicationContext as Application,
                "com.clevertap.android.geofence.CTGeofenceReceiver"
            )
            validateReceiverInManifest(
                context.applicationContext as Application,
                "com.clevertap.android.geofence.CTLocationUpdateReceiver"
            )
            validateReceiverInManifest(
                context.applicationContext as Application,
                "com.clevertap.android.geofence.CTGeofenceBootReceiver"
            )
        } catch (e: Exception) {
            Logger.v("Receiver/Service issue : $e")
        }
        val enabledPushTypes = pushProviders.getAvailablePushTypes()

        for (pushType in enabledPushTypes) {
            if (pushType === PushConstants.FCM) {
                try {
                    // use class name string directly here to avoid class not found issues on class import
                    validateServiceInManifest(
                        context.applicationContext as Application,
                        "com.clevertap.android.sdk.pushnotification.fcm.FcmMessageListenerService"
                    )
                } catch (e: Exception) {
                    Logger.v("Receiver/Service issue : $e")
                } catch (error: Error) {
                    Logger.v("FATAL : " + error.message)
                }
            }
        }
    }

    private fun checkSDKVersion(deviceInfo: DeviceInfo) {
        Logger.i("SDK Version Code is " + deviceInfo.sdkVersion)
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun validateActivityInManifest(application: Application, activityClass: Class<*>) {
        val pm = application.packageManager
        val packageName = application.packageName

        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        val activities = packageInfo.activities
        val activityClassName = activityClass.name
        for (activityInfo in activities!!) {
            if (activityInfo.name == activityClassName) {
                Logger.i(activityClassName.replaceFirst("com.clevertap.android.sdk.".toRegex(), "") + " is present")
                return
            }
        }
        Logger.i(activityClassName.replaceFirst("com.clevertap.android.sdk.".toRegex(), "") + " not present")
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun validateReceiverInManifest(application: Application, receiverClassName: String) {
        val pm = application.packageManager
        val packageName = application.packageName

        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_RECEIVERS)
        val receivers = packageInfo.receivers

        for (activityInfo in receivers!!) {
            if (activityInfo.name == receiverClassName) {
                Logger.i(receiverClassName.replaceFirst("com.clevertap.android.".toRegex(), "") + " is present")
                return
            }
        }
        Logger.i(receiverClassName.replaceFirst("com.clevertap.android.".toRegex(), "") + " not present")
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun validateServiceInManifest(application: Application, serviceClassName: String) {
        val pm = application.packageManager
        val packageName = application.packageName

        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES)
        val services = packageInfo.services
        for (serviceInfo in services!!) {
            if (serviceInfo.name == serviceClassName) {
                Logger.i(serviceClassName.replaceFirst("com.clevertap.android.sdk.".toRegex(), "") + " is present")
                return
            }
        }
        Logger.i(serviceClassName.replaceFirst("com.clevertap.android.sdk.".toRegex(), "") + " not present")
    }

    private fun validationApplicationLifecyleCallback(context: Context) {
        // some of the ancillary SDK wrappers have to manage the activity lifecycle manually because they don't have access to the application object or whatever
        // for those cases also consider CleverTapAPI.isAppForeground() as a proxy for the SDK being in sync with the activity lifecycle
        if (!ActivityLifecycleCallback.registered && !CleverTapAPI.isAppForeground()) {
            Logger.i(
                "Activity Lifecycle Callback not registered. Either set the android:name in your AndroidManifest.xml application tag to com.clevertap.android.sdk.Application, \n or, "
                        +
                        "if you have a custom Application class, call ActivityLifecycleCallback.register(this); before super.onCreate() in your class"
            )
            //Check for Application class only if the application lifecycle seems to be a problem
            checkApplicationClass(context)
        }
    }
}
