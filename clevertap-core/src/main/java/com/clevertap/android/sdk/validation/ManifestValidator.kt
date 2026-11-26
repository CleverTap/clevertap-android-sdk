package com.clevertap.android.sdk.validation

import android.app.Application
import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
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
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object ManifestValidator {

    private const val OUR_APPLICATION_CLASS_NAME = "com.clevertap.android.sdk.Application"

    private const val SDK_PACKAGE_PREFIX = "com.clevertap.android.sdk."

    private const val CLEVERTAP_PACKAGE_PREFIX = "com.clevertap.android."

    fun validate(context: Context, deviceInfo: DeviceInfo, pushProviders: PushProviders) {
        if (!Utils.hasPermission(context, "android.permission.INTERNET")) {
            Logger.d("Missing Permission: android.permission.INTERNET")
        }
        checkSDKVersion(deviceInfo)
        validationApplicationLifecyleCallback(context)
        checkReceiversServices(context, pushProviders)
        if (!ManifestInfo.getInstance(context).fcmSenderId.isNullOrEmpty()) {
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
        when {
            appName.isNullOrEmpty() -> Logger.i("Unable to determine Application Class")
            appName == OUR_APPLICATION_CLASS_NAME -> Logger.i(
                "AndroidManifest.xml uses the CleverTap Application class, " +
                    "be sure you have properly added the CleverTap Account ID and Token to your AndroidManifest.xml, \n" +
                    "or set them programmatically in the onCreate method of your custom application class prior to calling super.onCreate()"
            )

            else -> Logger.i("Application Class is $appName")
        }
    }

    private fun checkReceiversServices(context: Context, pushProviders: PushProviders) {
        try {
            // the cast stays inside the try: a non-Application context is reported, not thrown
            val application = context.applicationContext as Application
            validateReceiverInManifest(application, CTPushNotificationReceiver::class.java.name)
            validateServiceInManifest(application, CTNotificationIntentService::class.java.name)
            validateActivityInManifest(application, InAppNotificationActivity::class.java)
            validateActivityInManifest(application, CTInboxActivity::class.java)
            validateReceiverInManifest(application, "com.clevertap.android.geofence.CTGeofenceReceiver")
            validateReceiverInManifest(application, "com.clevertap.android.geofence.CTLocationUpdateReceiver")
            validateReceiverInManifest(application, "com.clevertap.android.geofence.CTGeofenceBootReceiver")
        } catch (e: Exception) {
            Logger.v("Receiver/Service issue : $e")
        }

        for (pushType in pushProviders.getAvailablePushTypes()) {
            if (pushType !== PushConstants.FCM) {
                continue
            }
            try {
                // use class name string directly here to avoid class not found issues on class import
                validateServiceInManifest(
                    context.applicationContext as Application,
                    "com.clevertap.android.sdk.pushnotification.fcm.FcmMessageListenerService"
                )
            } catch (e: Exception) {
                Logger.v("Receiver/Service issue : $e")
            } catch (error: Error) {
                Logger.v("FATAL : ${error.message}")
            }
        }
    }

    private fun checkSDKVersion(deviceInfo: DeviceInfo) {
        Logger.i("SDK Version Code is ${deviceInfo.sdkVersion}")
    }

    private fun validateActivityInManifest(application: Application, activityClass: Class<*>) {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, PackageManager.GET_ACTIVITIES)
        logPresence(packageInfo.activities, activityClass.name, SDK_PACKAGE_PREFIX)
    }

    private fun validateReceiverInManifest(application: Application, receiverClassName: String) {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, PackageManager.GET_RECEIVERS)
        logPresence(packageInfo.receivers, receiverClassName, CLEVERTAP_PACKAGE_PREFIX)
    }

    private fun validateServiceInManifest(application: Application, serviceClassName: String) {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, PackageManager.GET_SERVICES)
        logPresence(packageInfo.services, serviceClassName, SDK_PACKAGE_PREFIX)
    }

    /**
     * Logs whether [className] is declared among [components]. A null array (no component of that
     * kind declared) throws, as the Java original did, and is reported by the caller's catch.
     */
    private fun logPresence(components: Array<out ComponentInfo>?, className: String, prefix: String) {
        val isPresent = requireNotNull(components).any { it.name == className }
        val shortName = className.replaceFirst(prefix, "")
        Logger.i(if (isPresent) "$shortName is present" else "$shortName not present")
    }

    private fun validationApplicationLifecyleCallback(context: Context) {
        // some of the ancillary SDK wrappers have to manage the activity lifecycle manually because they don't have access to the application object or whatever
        // for those cases also consider CleverTapAPI.isAppForeground() as a proxy for the SDK being in sync with the activity lifecycle
        if (!ActivityLifecycleCallback.registered && !CleverTapAPI.isAppForeground()) {
            Logger.i(
                "Activity Lifecycle Callback not registered. Either set the android:name in your AndroidManifest.xml application tag to com.clevertap.android.sdk.Application, \n or, " +
                    "if you have a custom Application class, call ActivityLifecycleCallback.register(this); before super.onCreate() in your class"
            )
            //Check for Application class only if the application lifecycle seems to be a problem
            checkApplicationClass(context)
        }
    }
}
