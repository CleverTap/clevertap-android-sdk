package com.clevertap.android.sdk.validation

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
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType
import com.clevertap.android.sdk.pushnotification.PushProviders

object ManifestValidator {
    private const val ourApplicationClassName = "com.clevertap.android.sdk.Application"

    @JvmStatic
    fun validate(context: Context, deviceInfo: DeviceInfo, pushProviders: PushProviders) {
        if (!Utils.hasPermission(context, "android.permission.INTERNET")) {
            Logger.d("Missing Permission: android.permission.INTERNET")
        }
        checkSDKVersion(deviceInfo)
        validationApplicationLifecycleCallback(context)
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
            Logger.i("AndroidManifest.xml uses the CleverTap Application class, " +
                    "be sure you have properly added the CleverTap Account ID and Token to your AndroidManifest.xml, \n"
                    +
                    "or set them programmatically in the onCreate method of your custom application class prior to calling super.onCreate()")
        } else {
            Logger.i("Application Class is $appName")
        }
    }

    private fun checkReceiversServices(context: Context, pushProviders: PushProviders) {
        validateComponentInManifest(
            context.applicationContext,
            CTPushNotificationReceiver::class.java.name, ComponentType.RECEIVER
        )
        validateComponentInManifest(
            context.applicationContext,
            CTNotificationIntentService::class.java.name, ComponentType.SERVICE
        )
        validateComponentInManifest(
            context.applicationContext,
            InAppNotificationActivity::class.java.name, ComponentType.ACTIVITY
        )
        validateComponentInManifest(
            context.applicationContext,
            CTInboxActivity::class.java.name, ComponentType.ACTIVITY
        )
        validateComponentInManifest(
            context.applicationContext,
            "com.clevertap.android.geofence.CTGeofenceReceiver", ComponentType.RECEIVER
        )
        validateComponentInManifest(
            context.applicationContext,
            "com.clevertap.android.geofence.CTLocationUpdateReceiver", ComponentType.RECEIVER
        )
        validateComponentInManifest(
            context.applicationContext,
            "com.clevertap.android.geofence.CTGeofenceBootReceiver", ComponentType.RECEIVER
        )
        validateComponentInManifest(
            context.applicationContext,
            "com.clevertap.android.pushtemplates.TimerTemplateService", ComponentType.SERVICE
        )

        val enabledPushTypes = pushProviders.availablePushTypes

        for (pushType in enabledPushTypes) {
            if (pushType == PushType.FCM) {
                // use class name string directly here to avoid class not found issues on class import
                validateComponentInManifest(
                    context.applicationContext,
                    "com.clevertap.android.sdk.pushnotification.fcm.FcmMessageListenerService",
                    ComponentType.SERVICE
                )
            } else if (pushType == PushType.HPS) {
                // use class name string directly here to avoid class not found issues on class import
                validateComponentInManifest(
                    context.applicationContext,
                    "com.clevertap.android.hms.CTHmsMessageService", ComponentType.SERVICE
                )
            }
        }
    }

    private fun checkSDKVersion(deviceInfo: DeviceInfo) {
        Logger.i("SDK Version Code is " + deviceInfo.sdkVersion)
    }

    private fun validateComponentInManifest(
        context: Context,
        componentClassName: String,
        componentType: ComponentType
    ) {
        if (isComponentPresentInManifest(context, componentClassName, componentType)) {
            Logger.i(
                componentClassName.replaceFirst(
                    "com.clevertap.android.sdk.",
                    ""
                ) + " is present"
            )
        } else {
            Logger.i(
                componentClassName.replaceFirst(
                    "com.clevertap.android.sdk.",
                    ""
                ) + " not present"
            )
        }
    }

    @JvmStatic
    fun isComponentPresentInManifest(
        context: Context,
        componentClassName: String,
        componentType: ComponentType
    ): Boolean {
        val pm = context.packageManager
        val packageName = context.packageName

        return try {
            val packageInfo = pm.getPackageInfo(packageName, componentType.flag)
            val components = when (componentType) {
                ComponentType.SERVICE -> packageInfo.services
                ComponentType.RECEIVER -> packageInfo.receivers
                ComponentType.ACTIVITY -> packageInfo.activities
            }

            components?.any { it.name == componentClassName } ?: false
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.v("Issue in ${componentType.name.lowercase()}: $componentClassName - $e")
            false
        }
    }


    private fun validationApplicationLifecycleCallback(context: Context) {
        // some of the ancillary SDK wrappers have to manage the activity lifecycle manually because they don't have access to the application object or whatever
        // for those cases also consider CleverTapAPI.isAppForeground() as a proxy for the SDK being in sync with the activity lifecycle
        if (!ActivityLifecycleCallback.registered && !CleverTapAPI.isAppForeground()) {
            Logger.i(
                "Activity Lifecycle Callback not registered. Either set the android:name in your AndroidManifest.xml application tag to com.clevertap.android.sdk.Application, \n or, "
                        +
                        "if you have a custom Application class, call ActivityLifecycleCallback.register(this); before super.onCreate() in your class")
            //Check for Application class only if the application lifecycle seems to be a problem
            checkApplicationClass(context)
        }
    }

    enum class ComponentType(val flag: Int) {
        RECEIVER(PackageManager.GET_RECEIVERS),
        SERVICE(PackageManager.GET_SERVICES),
        ACTIVITY(PackageManager.GET_ACTIVITIES)
    }
}
