package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.CTXtensions.isPackageAndOsTargetsAbove;
import static com.clevertap.android.sdk.Constants.NOTIFICATION_PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.clevertap.android.sdk.inapp.AlertDialogPromptForSettings;
import kotlin.Unit;

/**
 *  This class abstracts notification permission request flow for Android 13+ devices. To call Android OS notification
 *  permission flow from activity, one should call `showHardPermissionPrompt()` which will request for
 *  notification permission and gives back result of the permission to the caller activity. *
 *
 * */
public class PushPermissionManager {

    private final CleverTapInstanceConfig config;

    private boolean isFallbackSettingsEnabled;

    public static final String ANDROID_PERMISSION_STRING = "android.permission.POST_NOTIFICATIONS";

    private final Activity activity;

    private boolean isFromNotificationSettingsActivity;

    public PushPermissionManager(Activity activity, CleverTapInstanceConfig config) {
        this.activity = activity;
        this.config = config;
        this.isFromNotificationSettingsActivity = false;
    }

    public boolean isFromNotificationSettingsActivity(){
        return isFromNotificationSettingsActivity;
    }

    @SuppressLint("NewApi")
    public void showHardPermissionPrompt(boolean isFallbackSettingsEnabled,
                                         InAppNotificationActivity.PushPermissionResultCallback
                                                 pushPermissionResultCallback){
        if (isPackageAndOsTargetsAbove(activity, 32)) {
            this.isFallbackSettingsEnabled = isFallbackSettingsEnabled;
            requestPermission(pushPermissionResultCallback);
        }
    }

    @RequiresApi(api = 33)
    public void requestPermission(InAppNotificationActivity.PushPermissionResultCallback pushPermissionResultCallback) {
        int permissionStatus = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.POST_NOTIFICATIONS);

        if (permissionStatus == PackageManager.PERMISSION_DENIED){
            boolean isFirstTimeRequest = CTPreferenceCache.getInstance(activity, config).isFirstTimeRequest();

            Activity currentActivity = CoreMetaData.getCurrentActivity();
            if (currentActivity == null) {
                Logger.d("CurrentActivity reference is null. SDK can't prompt the user with Notification Permission! Ensure the following things:\n" +
                        "1. Calling ActivityLifecycleCallback.register(this) in your custom application class before super.onCreate().\n" +
                        "   Alternatively, register CleverTap SDK's Application class in the manifest using com.clevertap.android.sdk.Application.\n" +
                        "2. Ensure that the promptPushPrimer() API is called from the onResume() lifecycle method, not onCreate().");
                return;
            }
            boolean shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    currentActivity,
                    ANDROID_PERMISSION_STRING);

            if (!isFirstTimeRequest && shouldShowRequestPermissionRationale){
                if (shouldShowFallbackAlertDialog()){
                    showFallbackAlertDialog();
                    return;
                }
            }

            ActivityCompat.requestPermissions(activity,
                    new String[]{ANDROID_PERMISSION_STRING}, NOTIFICATION_PERMISSION_REQUEST_CODE);
        }else{
            pushPermissionResultCallback.onPushPermissionAccept();
            if (activity instanceof InAppNotificationActivity) {
                ((InAppNotificationActivity) activity).didDismiss(null);
            }
        }
    }

    private boolean shouldShowFallbackAlertDialog() {
        return isFallbackSettingsEnabled;
    }

    public void showFallbackAlertDialog() {
        AlertDialogPromptForSettings.show(activity, () -> {
            Utils.navigateToAndroidSettingsForNotifications(activity);
            isFromNotificationSettingsActivity = true;
            return Unit.INSTANCE;
        }, () -> {
            if (activity instanceof InAppNotificationActivity) {
                ((InAppNotificationActivity) activity).notifyPermissionDenied();
                ((InAppNotificationActivity) activity).didDismiss(null);
            }
            return Unit.INSTANCE;
        });
    }
}
