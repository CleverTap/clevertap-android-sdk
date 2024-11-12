package com.clevertap.android.sdk.validation;

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
        validateComponentInManifest(context.getApplicationContext(),
                CTPushNotificationReceiver.class.getName(), ComponentType.RECEIVER);
        validateComponentInManifest(context.getApplicationContext(),
                CTNotificationIntentService.class.getName(), ComponentType.SERVICE);
        validateComponentInManifest(context.getApplicationContext(),
                InAppNotificationActivity.class.getName(), ComponentType.ACTIVITY);
        validateComponentInManifest(context.getApplicationContext(),
                CTInboxActivity.class.getName(), ComponentType.ACTIVITY);
        validateComponentInManifest(context.getApplicationContext(),
                "com.clevertap.android.geofence.CTGeofenceReceiver", ComponentType.RECEIVER);
        validateComponentInManifest(context.getApplicationContext(),
                "com.clevertap.android.geofence.CTLocationUpdateReceiver", ComponentType.RECEIVER);
        validateComponentInManifest(context.getApplicationContext(),
                "com.clevertap.android.geofence.CTGeofenceBootReceiver", ComponentType.RECEIVER);

        ArrayList<PushType> enabledPushTypes = pushProviders.getAvailablePushTypes();
        if (enabledPushTypes == null) {
            return;
        }

        for (PushType pushType : enabledPushTypes) {
            if (pushType == PushType.FCM) {
                // use class name string directly here to avoid class not found issues on class import
                validateComponentInManifest(context.getApplicationContext(),
                        "com.clevertap.android.sdk.pushnotification.fcm.FcmMessageListenerService", ComponentType.SERVICE);

            } else if (pushType == PushType.HPS) {
                // use class name string directly here to avoid class not found issues on class import
                validateComponentInManifest(context.getApplicationContext(),
                        "com.clevertap.android.hms.CTHmsMessageService", ComponentType.SERVICE);
            }
        }

    }

    private static void checkSDKVersion(DeviceInfo deviceInfo) {
        Logger.i("SDK Version Code is " + deviceInfo.getSdkVersion());
    }

    public enum ComponentType {
        RECEIVER(PackageManager.GET_RECEIVERS),
        SERVICE(PackageManager.GET_SERVICES),
        ACTIVITY(PackageManager.GET_ACTIVITIES);

        final int flag;

        ComponentType(int flag) {
            this.flag = flag;
        }
    }

    private static void validateComponentInManifest(Context context, String componentClassName, ComponentType componentType) {
        if (isComponentPresentInManifest(context, componentClassName, componentType)) {
            Logger.i(componentClassName.replaceFirst("com.clevertap.android.sdk.", "") + " is present");
        } else {
            Logger.i(componentClassName.replaceFirst("com.clevertap.android.sdk.", "") + " not present");
        }
    }

    public static boolean isComponentPresentInManifest(Context context, String componentClassName, ComponentType componentType) {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();

            PackageInfo packageInfo = pm.getPackageInfo(packageName, componentType.flag);
            if (componentType == ComponentType.SERVICE && packageInfo.services != null) {
                for (ServiceInfo serviceInfo : packageInfo.services) {
                    if (componentClassName.equals(serviceInfo.name)) {
                        return true;
                    }
                }
            } else if (componentType == ComponentType.RECEIVER && packageInfo.receivers != null) {
                for (ActivityInfo receiverInfo : packageInfo.receivers) {
                    if (componentClassName.equals(receiverInfo.name)) {
                        return true;
                    }
                }
            } else if(componentType == ComponentType.ACTIVITY && packageInfo.activities != null) {
                for (ActivityInfo activityInfo : packageInfo.activities) {
                    if (componentClassName.equals(activityInfo.name)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Logger.v("Issue in " + componentType.name().toLowerCase() + ": " + componentClassName + " - " + e);
        }
        return false;
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
