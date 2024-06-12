package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.Constants.NOTIFICATION_PERMISSION_REQUEST_CODE;
import static com.clevertap.android.sdk.inapp.InAppController.DISPLAY_HARD_PERMISSION_BUNDLE_KEY;
import static com.clevertap.android.sdk.inapp.InAppController.SHOW_FALLBACK_SETTINGS_BUNDLE_KEY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.clevertap.android.sdk.inapp.CTInAppAction;
import com.clevertap.android.sdk.inapp.CTInAppBaseFullFragment;
import com.clevertap.android.sdk.inapp.CTInAppHtmlCoverFragment;
import com.clevertap.android.sdk.inapp.CTInAppHtmlHalfInterstitialFragment;
import com.clevertap.android.sdk.inapp.CTInAppHtmlInterstitialFragment;
import com.clevertap.android.sdk.inapp.CTInAppNativeCoverFragment;
import com.clevertap.android.sdk.inapp.CTInAppNativeCoverImageFragment;
import com.clevertap.android.sdk.inapp.CTInAppNativeHalfInterstitialFragment;
import com.clevertap.android.sdk.inapp.CTInAppNativeHalfInterstitialImageFragment;
import com.clevertap.android.sdk.inapp.CTInAppNativeInterstitialFragment;
import com.clevertap.android.sdk.inapp.CTInAppNativeInterstitialImageFragment;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton;
import com.clevertap.android.sdk.inapp.CTInAppType;
import com.clevertap.android.sdk.inapp.InAppActionType;
import com.clevertap.android.sdk.inapp.InAppListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public final class InAppNotificationActivity extends FragmentActivity implements InAppListener,
        DidClickForHardPermissionListener {

    private static boolean isAlertVisible = false;

    private CleverTapInstanceConfig config;

    private CTInAppNotification inAppNotification;

    private WeakReference<InAppListener> listenerWeakReference;

    private WeakReference<PushPermissionResultCallback> pushPermissionResultCallbackWeakReference;

    private PushPermissionManager pushPermissionManager;

    private Bundle returnBundle = null;

    private boolean invokedInAppDismissCallback = false;

    public interface PushPermissionResultCallback {

        void onPushPermissionAccept();

        void onPushPermissionDeny();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        try {
            Bundle notif = getIntent().getExtras();
            if (notif == null) {
                throw new IllegalArgumentException();
            }
            inAppNotification = notif.getParcelable(Constants.INAPP_KEY);
            boolean showHardNotificationPermission = notif.getBoolean(DISPLAY_HARD_PERMISSION_BUNDLE_KEY,
                    false); // Using this boolean for a directly showing hard permission dialog flow
            Bundle configBundle = notif.getBundle("configBundle");
            if (configBundle != null) {
                config = configBundle.getParcelable("config");
            }

            setListener(CleverTapAPI.instanceWithConfig(this, config).getCoreState().getInAppController());
            setPermissionCallback(CleverTapAPI.instanceWithConfig(this, config).getCoreState()
                    .getInAppController());

            pushPermissionManager = new PushPermissionManager(this, config);

            if (showHardNotificationPermission) {
                boolean shouldShowFallbackSettings = notif.getBoolean(SHOW_FALLBACK_SETTINGS_BUNDLE_KEY,
                        false);
                showHardPermissionPrompt(shouldShowFallbackSettings);
                return;
            }
        } catch (Throwable t) {
            Logger.v("Cannot find a valid notification bundle to show!", t);
            finish();
            return;
        }
        if (inAppNotification == null) {
            finish();
            return;
        }

        //Allow rotation for all InApps but respect the flags sent from dashboard
        if (inAppNotification.isPortrait() && !inAppNotification.isLandscape()) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Logger.d("App in Landscape, dismissing portrait InApp Notification");
                finish();
                didDismiss(null);
                return;
            } else {
                Logger.d("App in Portrait, displaying InApp Notification anyway");
            }
        }

        if (!inAppNotification.isPortrait() && inAppNotification.isLandscape()) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                Logger.d("App in Portrait, dismissing landscape InApp Notification");
                finish();
                didDismiss(null);
                return;
            } else {
                Logger.d("App in Landscape, displaying InApp Notification anyway");
            }
        }

        CTInAppBaseFullFragment contentFragment;
        if (savedInstanceState == null) {
            contentFragment = createContentFragment();
            if (contentFragment != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("inApp", inAppNotification);
                bundle.putParcelable("config", config);
                contentFragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .add(android.R.id.content, contentFragment, getFragmentTag())
                        .commitNow();
            }
        } else if (isAlertVisible) {
            createContentFragment();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pushPermissionManager.isFromNotificationSettingsActivity()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                int permissionStatus = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.POST_NOTIFICATIONS);
                if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                    pushPermissionResultCallbackWeakReference.get().onPushPermissionAccept();
                } else {
                    pushPermissionResultCallbackWeakReference.get().onPushPermissionDeny();
                }
                didDismiss(null);
            }
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public void finish() {
        super.finish();
        if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }

        if (invokedInAppDismissCallback) {
            return;
        }
        notifyInAppDismissed();
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        if (invokedInAppDismissCallback) {
            return;
        }
        notifyInAppDismissed();
    }

    private void notifyInAppDismissed() {
        if (isAlertVisible) {
            isAlertVisible = false;
        }
        InAppListener listener = getListener();
        if (listener != null && getBaseContext() != null && inAppNotification != null) {
            listener.inAppNotificationDidDismiss(getBaseContext(), inAppNotification, returnBundle);
        }
        invokedInAppDismissCallback = true;
    }

    @Nullable
    @Override
    public Bundle inAppNotificationDidClick(
            @NonNull final CTInAppNotification inAppNotification,
            @NonNull final CTInAppNotificationButton button,
            @Nullable final Context activityContext) {
        InAppListener listener = getListener();
        if (listener != null) {
            return listener.inAppNotificationDidClick(inAppNotification, button, this);
        } else {
            return null;
        }
    }

    @Override
    public void inAppNotificationDidDismiss(
            @NonNull final Context context,
            @NonNull final CTInAppNotification inAppNotification,
            @Nullable Bundle formData) {
        didDismiss(formData);
    }

    @Override
    public void inAppNotificationDidShow(@NonNull CTInAppNotification inAppNotification, @Nullable Bundle formData) {
        didShow(formData);
    }

    @Nullable
    @Override
    public Bundle inAppNotificationActionTriggered(
            @NonNull final CTInAppNotification inAppNotification,
            @NonNull final CTInAppAction action,
            @NonNull final String callToAction,
            @Nullable final Bundle additionalData,
            @Nullable final Context activityContext) {
        InAppListener listener = getListener();
        if (listener != null) {
            return listener.inAppNotificationActionTriggered(
                    inAppNotification,
                    action,
                    callToAction,
                    additionalData,
                    this);
        } else {
            return null;
        }
    }

    @Override
    public void setTheme(int resid) {
        super.setTheme(android.R.style.Theme_Translucent_NoTitleBar);
    }

    @Override
    public void didClickForHardPermissionWithFallbackSettings(boolean fallbackToSettings) {
        showHardPermissionPrompt(fallbackToSettings);
    }

    @SuppressLint("NewApi")
    public void showHardPermissionPrompt(boolean isFallbackSettingsEnabled) {
        pushPermissionManager.showHardPermissionPrompt(isFallbackSettingsEnabled,
                pushPermissionResultCallbackWeakReference.get());
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CTPreferenceCache.getInstance(this, config).setFirstTimeRequest(false);
        CTPreferenceCache.updateCacheToDisk(this, config);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED;
            if (granted) {
                pushPermissionResultCallbackWeakReference.get().onPushPermissionAccept();
            } else {
                pushPermissionResultCallbackWeakReference.get().onPushPermissionDeny();
            }
            didDismiss(null);
        }
    }

    void didDismiss(Bundle data) {
        returnBundle = data;
        finish();
    }

    void didShow(Bundle data) {
        InAppListener listener = getListener();
        if (listener != null) {
            listener.inAppNotificationDidShow(inAppNotification, data);
        }
    }

    InAppListener getListener() {
        InAppListener listener = null;
        try {
            listener = listenerWeakReference.get();
        } catch (Throwable t) {
            // no-op
        }
        if (listener == null) {
            config.getLogger().verbose(config.getAccountId(),
                    "InAppActivityListener is null for notification: " + inAppNotification.getJsonDescription());
        }
        return listener;
    }

    void setListener(InAppListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
    }

    public void setPermissionCallback(PushPermissionResultCallback callback) {
        pushPermissionResultCallbackWeakReference = new WeakReference<>(callback);
    }

    public void notifyPermissionDenied() {
        pushPermissionResultCallbackWeakReference.get().onPushPermissionDeny();
    }

    @Nullable
    private Bundle didClick(CTInAppNotificationButton button) {
        InAppListener listener = getListener();
        if (listener != null) {
            return listener.inAppNotificationDidClick(inAppNotification, button, this);
        } else {
            return null;
        }
    }

    private CTInAppBaseFullFragment createContentFragment() {
        CTInAppType type = inAppNotification.getInAppType();
        CTInAppBaseFullFragment viewFragment = null;
        switch (type) {
            case CTInAppTypeCoverHTML: {
                viewFragment = new CTInAppHtmlCoverFragment();
                break;
            }
            case CTInAppTypeInterstitialHTML: {
                viewFragment = new CTInAppHtmlInterstitialFragment();
                break;
            }
            case CTInAppTypeHalfInterstitialHTML: {
                viewFragment = new CTInAppHtmlHalfInterstitialFragment();
                break;
            }
            case CTInAppTypeCover: {
                viewFragment = new CTInAppNativeCoverFragment();
                break;
            }
            case CTInAppTypeInterstitial: {
                viewFragment = new CTInAppNativeInterstitialFragment();
                break;
            }
            case CTInAppTypeHalfInterstitial: {
                viewFragment = new CTInAppNativeHalfInterstitialFragment();
                break;
            }
            case CTInAppTypeCoverImageOnly: {
                viewFragment = new CTInAppNativeCoverImageFragment();
                break;
            }
            case CTInAppTypeInterstitialImageOnly: {
                viewFragment = new CTInAppNativeInterstitialImageFragment();
                break;
            }
            case CTInAppTypeHalfInterstitialImageOnly: {
                viewFragment = new CTInAppNativeHalfInterstitialImageFragment();
                break;
            }
            case CTInAppTypeAlert: {
                showAlertDialogForInApp();
                break;
            }
            default: {
                config.getLogger().verbose("InAppNotificationActivity: Unhandled InApp Type: " + type);
                break;
            }
        }
        return viewFragment;
    }

    private String getFragmentTag() {
        return config.getAccountId() + ":CT_INAPP_CONTENT_FRAGMENT";
    }

    private void showAlertDialogForInApp() {
        ArrayList<CTInAppNotificationButton> buttons = inAppNotification.getButtons();
        if (buttons.isEmpty()) {
            config.getLogger()
                    .debug("InAppNotificationActivity: Notification has no buttons, not showing Alert InApp");
            return;
        }
        AlertDialog alertDialog;
        CTInAppNotificationButton positiveButton = buttons.get(0);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            alertDialog = new AlertDialog.Builder(InAppNotificationActivity.this,
                    android.R.style.Theme_Material_Light_Dialog_Alert)
                    .setCancelable(false)
                    .setTitle(inAppNotification.getTitle())
                    .setMessage(inAppNotification.getMessage())
                    .setPositiveButton(positiveButton.getText(),
                            (dialogInterface, i) -> onAlertButtonClick(positiveButton, true))
                    .create();

            if (inAppNotification.getButtons().size() == 2) {
                CTInAppNotificationButton negativeButton = buttons.get(1);
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                        negativeButton.getText(),
                        (dialog, which) -> onAlertButtonClick(negativeButton, false));
            }
        } else {
            alertDialog = new AlertDialog.Builder(InAppNotificationActivity.this)
                    .setCancelable(false)
                    .setTitle(inAppNotification.getTitle())
                    .setMessage(inAppNotification.getMessage())
                    .setPositiveButton(positiveButton.getText(),
                            (dialogInterface, i) -> onAlertButtonClickLegacy(positiveButton))
                    .create();

            if (inAppNotification.getButtons().size() == 2) {
                CTInAppNotificationButton negativeButton = buttons.get(1);
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                        negativeButton.getText(),
                        (dialog, which) -> onAlertButtonClickLegacy(negativeButton));
            }
        }
        //By default, we will allow 2 button alerts and set a third button if it is configured
        if (buttons.size() > 2) {
            CTInAppNotificationButton button = buttons.get(2);
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                    button.getText(),
                    (dialogInterface, i) -> onAlertButtonClickLegacy(button));
        }

        alertDialog.show();
        isAlertVisible = true;
        didShow(null);
    }

    private void onAlertButtonClickLegacy(final CTInAppNotificationButton button) {
        Bundle clickData = didClick(button);
        didDismiss(clickData);
    }

    private void onAlertButtonClick(CTInAppNotificationButton button, boolean isPositive) {
        Bundle clickData = didClick(button);

        if (isPositive && inAppNotification.isLocalInApp()) {
            showHardPermissionPrompt(inAppNotification.fallBackToNotificationSettings());
            return;
        }

        CTInAppAction action = button.getAction();
        if (action != null && InAppActionType.REQUEST_FOR_PERMISSIONS == action.getType()) {
            showHardPermissionPrompt(action.shouldFallbackToSettings());
            return;
        }

        didDismiss(clickData);
    }
}
