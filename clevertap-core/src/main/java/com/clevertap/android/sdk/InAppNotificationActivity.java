package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
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
        DidClickForHardPermissionListener, PushPermissionHandler.PushPermissionResultCallback {

    private final static String INTENT_EXTRA_DISPLAY_PUSH_PERMISSION_PROMPT = "displayPushPermissionPrompt";
    private final static String INTENT_EXTRA_PUSH_PERMISSION_FALLBACK_TO_SETTINGS = "shouldShowFallbackSettings";
    private final static String INTENT_EXTRA_CT_CONFIG = "config";

    private static boolean isAlertVisible = false;

    private CleverTapInstanceConfig config;

    private CTInAppNotification inAppNotification;

    private WeakReference<InAppListener> listenerWeakReference;

    private PushPermissionHandler pushPermissionHandler;

    private boolean invokedCallbacks = false;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void launchForPushPermissionPrompt(
            Activity activity,
            CleverTapInstanceConfig config,
            boolean showFallbackSettings) {
        if (!activity.getClass().equals(InAppNotificationActivity.class)) {
            Intent intent = new Intent(activity, InAppNotificationActivity.class);
            intent.putExtra(INTENT_EXTRA_CT_CONFIG, config);
            intent.putExtra(INTENT_EXTRA_DISPLAY_PUSH_PERMISSION_PROMPT, true);
            intent.putExtra(INTENT_EXTRA_PUSH_PERMISSION_FALLBACK_TO_SETTINGS, showFallbackSettings);
            activity.startActivity(intent);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void launchForInAppNotification(
            Context context,
            CTInAppNotification inAppNotification,
            CleverTapInstanceConfig config) {
        Intent intent = new Intent(context, InAppNotificationActivity.class);
        intent.putExtra(Constants.INAPP_KEY, inAppNotification);
        intent.putExtra(INTENT_EXTRA_CT_CONFIG, config);
        context.startActivity(intent);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                didDismiss(null);
            }
        });

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Window window = getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
                insetsController.hide(WindowInsetsCompat.Type.systemBars());
            }
        }
        try {
            Bundle intentExtras = getIntent().getExtras();
            if (intentExtras == null) {
                throw new IllegalArgumentException();
            }


            config = intentExtras.getParcelable(INTENT_EXTRA_CT_CONFIG);
            if (config == null) {
                throw new IllegalArgumentException();
            }

            CoreState ctState = CleverTapAPI.instanceWithConfig(this, config).getCoreState();

            pushPermissionHandler = new PushPermissionHandler(
                    config,
                    ctState.getCallbackManager().getPushPermissionResponseListenerList(),
                    this);

            boolean showHardNotificationPermission = intentExtras.getBoolean(
                    INTENT_EXTRA_DISPLAY_PUSH_PERMISSION_PROMPT,
                    false);
            if (showHardNotificationPermission) {
                boolean shouldShowFallbackSettings = intentExtras.getBoolean(
                        INTENT_EXTRA_PUSH_PERMISSION_FALLBACK_TO_SETTINGS,
                        false);
                showPushPermissionPrompt(shouldShowFallbackSettings);
                return;
            }

            setListener(ctState.getInAppController());

            inAppNotification = intentExtras.getParcelable(Constants.INAPP_KEY);
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
                contentFragment.setArguments(inAppNotification, config);
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
        pushPermissionHandler.onActivityResume(this);
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
        showPushPermissionPrompt(fallbackToSettings);
    }

    public void showPushPermissionPrompt(boolean isFallbackSettingsEnabled) {
        pushPermissionHandler.requestPermission(this, isFallbackSettingsEnabled);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        pushPermissionHandler.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onPushPermissionResult(boolean isGranted) {
        didDismiss(null);
    }

    void didDismiss(Bundle data) {
        didDismiss(data, true);
    }

    void didDismiss(Bundle data, boolean killActivity) {
        if (isAlertVisible) {
            isAlertVisible = false;
        }

        if (!invokedCallbacks) {
            InAppListener listener = getListener();
            if (listener != null && inAppNotification != null) {
                listener.inAppNotificationDidDismiss(inAppNotification, data);
            }
            invokedCallbacks = true;
        }
        if (killActivity) {
            finish();
        }
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
        if (listener == null && inAppNotification != null) {
            config.getLogger().verbose(config.getAccountId(),
                    "InAppActivityListener is null for notification: " + inAppNotification.getJsonDescription());
        }
        return listener;
    }

    void setListener(InAppListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
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
            showPushPermissionPrompt(inAppNotification.fallBackToNotificationSettings());
            return;
        }

        CTInAppAction action = button.getAction();
        if (action != null && InAppActionType.REQUEST_FOR_PERMISSIONS == action.getType()) {
            showPushPermissionPrompt(action.shouldFallbackToSettings());
            return;
        }

        didDismiss(clickData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            didDismiss(null, false);
        }
    }
}