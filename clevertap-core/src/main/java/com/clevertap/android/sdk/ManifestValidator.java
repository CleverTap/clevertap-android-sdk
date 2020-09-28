package com.clevertap.android.sdk;

import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationReceiver;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import com.clevertap.android.sdk.pushnotification.amp.CTBackgroundIntentService;
import com.clevertap.android.sdk.pushnotification.amp.CTBackgroundJobService;
import java.util.ArrayList;


final class ManifestValidator {

    private final static String ourApplicationClassName = "com.clevertap.android.sdk.Application";

    static void validate(final Context context, DeviceInfo deviceInfo, PushProviders pushProviders) {
        if (!Utils.hasPermission(context, "android.permission.INTERNET")) {
            Logger.d("Missing Permission: android.permission.INTERNET");
        }
        checkSDKVersion(deviceInfo);
        validationApplicationLifecyleCallback(context);
        checkReceiversServices(context, pushProviders);
    }

    private static void checkApplicationClass(final Context context) {
        String appName = context.getApplicationInfo().className;
        if (appName == null || appName.isEmpty()) {
            Logger.i("Unable to determine Application Class");
        } else if (appName.equals(ourApplicationClassName)) {
            Logger.i("AndroidManifest.xml uses the CleverTap Application class, " +
                    "be sure you have properly added the CleverTap Account ID and Token to your AndroidManifest.xml, \n"
                    +
                    "or set them programmatically in the onCreate method of your custom application class prior to calling super.onCreate()");
        } else {
            Logger.i("Application Class is " + appName);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void checkReceiversServices(final Context context, PushProviders pushProviders) {
        try {
            validateReceiverInManifest((Application) context.getApplicationContext(),
                    CTPushNotificationReceiver.class.getName());
            validateServiceInManifest((Application) context.getApplicationContext(),
                    CTNotificationIntentService.class.getName());
            validateServiceInManifest((Application) context.getApplicationContext(),
                    CTBackgroundJobService.class.getName());
            validateServiceInManifest((Application) context.getApplicationContext(),
                    CTBackgroundIntentService.class.getName());
            validateActivityInManifest((Application) context.getApplicationContext(),
                    InAppNotificationActivity.class);
        } catch (Exception e) {
            Logger.v("Receiver/Service issue : " + e.toString());

        }
        ArrayList<PushType> enabledPushTypes = pushProviders.getAvailablePushTypes();
        if (enabledPushTypes == null) {
            return;
        }
        for (PushType pushType : enabledPushTypes) {
            //no-op
            if (pushType == PushType.FCM) {
                try {
                    // use class name string directly here to avoid class not found issues on class import, because we only use FCM
                    validateServiceInManifest((Application) context.getApplicationContext(),
                            "com.clevertap.android.sdk.pushnotification.fcm.FcmMessageListenerService");
                    validateServiceInManifest((Application) context.getApplicationContext(),
                            "com.clevertap.android.sdk.FcmTokenListenerService");
                } catch (Exception e) {
                    Logger.v("Receiver/Service issue : " + e.toString());

                } catch (Error error) {
                    Logger.v("FATAL : " + error.getMessage());

                }
            }
        }

    }

    private static void checkSDKVersion(DeviceInfo deviceInfo) {
        Logger.i("SDK Version Code is " + deviceInfo.getSdkVersion());
    }

    @SuppressWarnings({"SameParameterValue", "rawtypes"})
    private static void validateActivityInManifest(Application application, Class activityClass)
            throws PackageManager.NameNotFoundException {
        PackageManager pm = application.getPackageManager();
        String packageName = application.getPackageName();

        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        ActivityInfo[] activities = packageInfo.activities;
        String activityClassName = activityClass.getName();
        for (ActivityInfo activityInfo : activities) {
            if (activityInfo.name.equals(activityClassName)) {
                Logger.i(activityClassName.replaceFirst("com.clevertap.android.sdk.", "") + " is present");
                return;
            }
        }
        Logger.i(activityClassName.replaceFirst("com.clevertap.android.sdk.", "") + " not present");
    }

    private static void validateReceiverInManifest(Application application, String receiverClassName)
            throws PackageManager.NameNotFoundException {
        PackageManager pm = application.getPackageManager();
        String packageName = application.getPackageName();

        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_RECEIVERS);
        ActivityInfo[] receivers = packageInfo.receivers;

        for (ActivityInfo activityInfo : receivers) {
            if (activityInfo.name.equals(receiverClassName)) {
                Logger.i(receiverClassName.replaceFirst("com.clevertap.android.sdk.", "") + " is present");
                return;
            }
        }
        Logger.i(receiverClassName.replaceFirst("com.clevertap.android.sdk.", "") + " not present");
    }

    private static void validateServiceInManifest(Application application, String serviceClassName)
            throws PackageManager.NameNotFoundException {
        PackageManager pm = application.getPackageManager();
        String packageName = application.getPackageName();

        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES);
        ServiceInfo[] services = packageInfo.services;
        for (ServiceInfo serviceInfo : services) {
            if (serviceInfo.name.equals(serviceClassName)) {
                Logger.i(serviceClassName.replaceFirst("com.clevertap.android.sdk.", "") + " is present");
                return;
            }
        }
        Logger.i(serviceClassName.replaceFirst("com.clevertap.android.sdk.", "") + " not present");
    }

    private static void validationApplicationLifecyleCallback(final Context context) {
        // some of the ancillary SDK wrappers have to manage the activity lifecycle manually because they don't have access to the application object or whatever
        // for those cases also consider CleverTapAPI.isAppForeground() as a proxy for the SDK being in sync with the activity lifecycle
        if (!ActivityLifecycleCallback.registered && !CleverTapAPI.isAppForeground()) {
            Logger.i(
                    "Activity Lifecycle Callback not registered. Either set the android:name in your AndroidManifest.xml application tag to com.clevertap.android.sdk.Application, \n or, "
                            +
                            "if you have a custom Application class, call ActivityLifecycleCallback.register(this); before super.onCreate() in your class");
            //Check for Application class only if the application lifecycle seems to be a problem
            checkApplicationClass(context);
        }
    }
}
