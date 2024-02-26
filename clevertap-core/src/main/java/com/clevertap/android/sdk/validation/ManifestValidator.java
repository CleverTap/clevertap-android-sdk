package com.clevertap.android.sdk.validation;

import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;
import com.clevertap.android.sdk.ActivityLifecycleCallback;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.InAppNotificationActivity;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.inbox.CTInboxActivity;
import com.clevertap.android.sdk.pushnotification.CTNotificationIntentService;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationReceiver;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import java.util.ArrayList;


public final class ManifestValidator {

    private final static String ourApplicationClassName = "com.clevertap.android.sdk.Application";

    public static void validate(final Context context, DeviceInfo deviceInfo, PushProviders pushProviders) {
        if (!Utils.hasPermission(context, "android.permission.INTERNET")) {
            Logger.d("Missing Permission: android.permission.INTERNET");
        }
        checkSDKVersion(deviceInfo);
        validationApplicationLifecyleCallback(context);
        checkReceiversServices(context, pushProviders);
        if (!TextUtils.isEmpty(ManifestInfo.getInstance(context).getFCMSenderId())){
            Logger.i("We have noticed that your app is using a custom FCM Sender ID, this feature will " +
                    "be DISCONTINUED from the next version of the CleverTap Android SDK. With the next release, " +
                    "CleverTap Android SDK will only fetch the token using the google-services.json." +
                    " Please reach out to CleverTap Support for any questions.");
        }
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
            validateActivityInManifest((Application) context.getApplicationContext(),
                    InAppNotificationActivity.class);
            validateActivityInManifest((Application) context.getApplicationContext(),
                    CTInboxActivity.class);
            validateReceiverInManifest((Application) context.getApplicationContext(),
                    "com.clevertap.android.geofence.CTGeofenceReceiver");
            validateReceiverInManifest((Application) context.getApplicationContext(),
                    "com.clevertap.android.geofence.CTLocationUpdateReceiver");
            validateReceiverInManifest((Application) context.getApplicationContext(),
                    "com.clevertap.android.geofence.CTGeofenceBootReceiver");
        } catch (Exception e) {
            Logger.v("Receiver/Service issue : " + e.toString());
        }
        ArrayList<PushType> enabledPushTypes = pushProviders.getAvailablePushTypes();
        if (enabledPushTypes == null) {
            return;
        }

        for (PushType pushType : enabledPushTypes) {
            if (pushType == PushType.FCM) {
                try {
                    // use class name string directly here to avoid class not found issues on class import
                    validateServiceInManifest((Application) context.getApplicationContext(),
                            "com.clevertap.android.sdk.pushnotification.fcm.FcmMessageListenerService");
                } catch (Exception e) {
                    Logger.v("Receiver/Service issue : " + e.toString());

                } catch (Error error) {
                    Logger.v("FATAL : " + error.getMessage());
                }
            }else if(pushType == PushType.HPS){
                try {
                    // use class name string directly here to avoid class not found issues on class import
                    validateServiceInManifest((Application) context.getApplicationContext(),
                            "com.clevertap.android.hms.CTHmsMessageService");
                } catch (Exception e) {
                    Logger.v("Receiver/Service issue : " + e.toString());

                } catch (Error error) {
                    Logger.v("FATAL : " + error.getMessage());
                }
            }else if(pushType == PushType.XPS){
                try {
                    // use class name string directly here to avoid class not found issues on class import
                    validateReceiverInManifest((Application) context.getApplicationContext(),
                            "com.clevertap.android.xps.XiaomiMessageReceiver");
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
                Logger.i(receiverClassName.replaceFirst("com.clevertap.android.", "") + " is present");
                return;
            }
        }
        Logger.i(receiverClassName.replaceFirst("com.clevertap.android.", "") + " not present");
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
